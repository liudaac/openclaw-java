package openclaw.tools.db;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Database query tool for executing SQL queries.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DatabaseQueryTool implements AgentTool {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean readOnly;

    public DatabaseQueryTool(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, true);
    }

    public DatabaseQueryTool(String jdbcUrl, String username, String password, boolean readOnly) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.readOnly = readOnly;
    }

    @Override
    public String getName() {
        return "database_query";
    }

    @Override
    public String getDescription() {
        return "Execute SQL queries against a database";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "query", PropertySchema.string("The SQL query to execute"),
                        "params", PropertySchema.array("Query parameters", PropertySchema.string("Parameter")),
                        "max_rows", PropertySchema.integer("Maximum rows to return (default 100)")
                ))
                .required(List.of("query"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String query = args.get("query").toString();

                // Security check
                ToolResult securityCheck = checkSecurity(query);
                if (securityCheck != null) {
                    return securityCheck;
                }

                List<String> params = (List<String>) args.getOrDefault("params", List.of());
                int maxRows = (int) args.getOrDefault("max_rows", 100);

                return executeQuery(query, params, maxRows);
            } catch (Exception e) {
                return ToolResult.failure("Database query failed: " + e.getMessage());
            }
        });
    }

    private ToolResult checkSecurity(String query) {
        String lowerQuery = query.toLowerCase().trim();

        // Check for dangerous operations in read-only mode
        if (readOnly) {
            Set<String> writeOps = Set.of(
                    "insert", "update", "delete", "drop", "create",
                    "alter", "truncate", "grant", "revoke"
            );
            for (String op : writeOps) {
                if (lowerQuery.startsWith(op)) {
                    return ToolResult.failure("Write operations not allowed in read-only mode: " + op);
                }
            }
        }

        // Check for dangerous patterns
        Set<String> dangerous = Set.of(
                "; drop", "; delete", "exec(", "execute(",
                "xp_", "sp_", "union select", "--", "/*"
        );
        for (String pattern : dangerous) {
            if (lowerQuery.contains(pattern)) {
                return ToolResult.failure("Potentially dangerous SQL pattern detected: " + pattern);
            }
        }

        return null; // Passed security check
    }

    private ToolResult executeQuery(String query, List<String> params, int maxRows) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            // Limit rows
            stmt.setMaxRows(maxRows);

            boolean hasResults = stmt.execute();

            if (hasResults) {
                // SELECT query
                try (ResultSet rs = stmt.getResultSet()) {
                    return formatResultSet(rs);
                }
            } else {
                // UPDATE/INSERT/DELETE
                int updateCount = stmt.getUpdateCount();
                return ToolResult.success("Query executed successfully", Map.of(
                        "rows_affected", updateCount
                ));
            }
        } catch (SQLException e) {
            return ToolResult.failure("SQL error: " + e.getMessage());
        }
    }

    private ToolResult formatResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Get column names
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnName(i));
        }

        // Get rows
        List<Map<String, Object>> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < 1000) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
            rowCount++;
        }

        // Format as string
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.join(" | ", columns)).append("\n");
        sb.append("-".repeat(sb.length())).append("\n");

        // Rows
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String col : columns) {
                Object val = row.get(col);
                values.add(val != null ? val.toString() : "NULL");
            }
            sb.append(String.join(" | ", values)).append("\n");
        }

        return ToolResult.success(sb.toString(), Map.of(
                "row_count", rows.size(),
                "columns", columns
        ));
    }
}
