package openclaw.memory.manager;

import openclaw.memory.embedding.EmbeddingProvider;
import openclaw.memory.search.MemorySearchEngine;
import openclaw.memory.search.MemorySearchResult;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite-based memory search engine with vector support.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class SQLiteMemorySearchEngine implements MemorySearchEngine, AutoCloseable {

    private final EmbeddingProvider embeddingProvider;
    private final Path dataDir;
    private Connection connection;

    public SQLiteMemorySearchEngine(EmbeddingProvider embeddingProvider, Path dataDir) {
        this.embeddingProvider = embeddingProvider;
        this.dataDir = dataDir;
    }

    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure data directory exists
                dataDir.toFile().mkdirs();
                
                // Connect to SQLite
                String dbPath = dataDir.resolve("memory.db").toString();
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                
                // Create tables
                createTables();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize SQLite memory engine", e);
            }
        });
    }

    private void createTables() throws SQLException {
        // Memories table
        String createMemoriesTable = """
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                source TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER,
                metadata TEXT
            )
            """;
        
        // Embeddings table
        String createEmbeddingsTable = """
            CREATE TABLE IF NOT EXISTS embeddings (
                id TEXT PRIMARY KEY,
                memory_id TEXT NOT NULL,
                vector BLOB NOT NULL,
                model TEXT NOT NULL,
                provider TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMemoriesTable);
            stmt.execute(createEmbeddingsTable);
        }
    }

    @Override
    public CompletableFuture<List<MemorySearchResult>> search(String query, SearchOptions options) {
        return embeddingProvider.embed(query)
                .thenCompose(vector -> searchVector(vector.vector(), options));
    }

    @Override
    public CompletableFuture<List<MemorySearchResult>> searchVector(float[] queryVector, SearchOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doSearchVector(queryVector, options);
            } catch (SQLException e) {
                throw new RuntimeException("Search failed", e);
            }
        });
    }

    private List<MemorySearchResult> doSearchVector(float[] queryVector, SearchOptions options) throws SQLException {
        List<MemorySearchResult> results = new ArrayList<>();
        
        // Simple cosine similarity search
        // In production, use sqlite-vec extension for efficient vector search
        String sql = """
            SELECT m.id, m.content, m.source, m.metadata, e.vector
            FROM memories m
            JOIN embeddings e ON m.id = e.memory_id
            LIMIT ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, options.limit());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String source = rs.getString("source");
                    String metadataJson = rs.getString("metadata");
                    byte[] vectorBytes = rs.getBytes("vector");
                    
                    float[] vector = bytesToFloats(vectorBytes);
                    double score = cosineSimilarity(queryVector, vector);
                    
                    if (score >= options.minScore()) {
                        Map<String, Object> metadata = parseMetadata(metadataJson);
                        results.add(new MemorySearchResult(content, score, source, metadata));
                    }
                }
            }
        }
        
        // Sort by score descending
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        
        return results;
    }

    @Override
    public CompletableFuture<String> addMemory(String content, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String id = generateId();
                long now = System.currentTimeMillis();
                
                // Insert memory
                String insertMemory = """
                    INSERT INTO memories (id, content, source, created_at, updated_at, metadata)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(insertMemory)) {
                    stmt.setString(1, id);
                    stmt.setString(2, content);
                    stmt.setString(3, metadata.getOrDefault("source", "unknown").toString());
                    stmt.setLong(4, now);
                    stmt.setLong(5, now);
                    stmt.setString(6, toJson(metadata));
                    stmt.executeUpdate();
                }
                
                // Generate embedding
                embeddingProvider.embed(content)
                        .thenAccept(vector -> {
                            try {
                                insertEmbedding(id, vector);
                            } catch (SQLException e) {
                                throw new RuntimeException("Failed to insert embedding", e);
                            }
                        })
                        .join();
                
                return id;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add memory", e);
            }
        });
    }

    private void insertEmbedding(String memoryId, EmbeddingProvider.EmbeddingVector vector) throws SQLException {
        String insertEmbedding = """
            INSERT INTO embeddings (id, memory_id, vector, model, provider, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertEmbedding)) {
            stmt.setString(1, generateId());
            stmt.setString(2, memoryId);
            stmt.setBytes(3, floatsToBytes(vector.vector()));
            stmt.setString(4, vector.model());
            stmt.setString(5, vector.provider());
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    @Override
    public CompletableFuture<Void> deleteMemory(String entryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM memories WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, entryId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete memory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateMemory(String entryId, String content, Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> {
            try {
                long now = System.currentTimeMillis();
                String sql = """
                    UPDATE memories 
                    SET content = ?, updated_at = ?, metadata = ?
                    WHERE id = ?
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, content);
                    stmt.setLong(2, now);
                    stmt.setString(3, toJson(metadata));
                    stmt.setString(4, entryId);
                    stmt.executeUpdate();
                }
                
                // Update embedding
                deleteEmbedding(entryId);
                embeddingProvider.embed(content)
                        .thenAccept(vector -> {
                            try {
                                insertEmbedding(entryId, vector);
                            } catch (SQLException e) {
                                throw new RuntimeException("Failed to update embedding", e);
                            }
                        })
                        .join();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update memory", e);
            }
        });
    }

    private void deleteEmbedding(String memoryId) throws SQLException {
        String sql = "DELETE FROM embeddings WHERE memory_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, memoryId);
            stmt.executeUpdate();
        }
    }

    public CompletableFuture<MemoryManager.MemoryStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalEntries = 0;
                int totalEmbeddings = 0;
                
                try (Statement stmt = connection.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM memories")) {
                        if (rs.next()) {
                            totalEntries = rs.getInt(1);
                        }
                    }
                    
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM embeddings")) {
                        if (rs.next()) {
                            totalEmbeddings = rs.getInt(1);
                        }
                    }
                }
                
                return new MemoryManager.MemoryStats(
                        totalEntries,
                        totalEmbeddings,
                        Optional.empty(),
                        0
                );
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get stats", e);
            }
        });
    }

    public CompletableFuture<Void> reindex() {
        return CompletableFuture.runAsync(() -> {
            // Reindex all memories
            try {
                String sql = "SELECT id, content FROM memories";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String content = rs.getString("content");
                        
                        deleteEmbedding(id);
                        embeddingProvider.embed(content)
                                .thenAccept(vector -> {
                                    try {
                                        insertEmbedding(id, vector);
                                    } catch (SQLException e) {
                                        throw new RuntimeException("Failed to reindex", e);
                                    }
                                })
                                .join();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to reindex", e);
            }
        });
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Log error
            }
        }
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    private byte[] floatsToBytes(float[] floats) {
        byte[] bytes = new byte[floats.length * 4];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return bytes;
    }

    private float[] bytesToFloats(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String toJson(Map<String, Object> metadata) {
        // Simple JSON serialization
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseMetadata(String json) {
        // Simple JSON parsing - in production use Jackson
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        return Map.of();
    }
}
