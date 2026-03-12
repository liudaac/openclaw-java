package openclaw.tools.search;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Web search tool interface.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface WebSearchTool extends AgentTool {

    /**
     * Searches the web.
     *
     * @param query the search query
     * @return the search results
     */
    CompletableFuture<SearchResults> search(String query);

    /**
     * Searches with options.
     *
     * @param query the query
     * @param options the search options
     * @return the results
     */
    CompletableFuture<SearchResults> searchWithOptions(String query, SearchOptions options);

    /**
     * Fetches a URL.
     *
     * @param url the URL
     * @return the fetched content
     */
    CompletableFuture<FetchResult> fetch(String url);

    @Override
    default String getName() {
        return "web_search";
    }

    @Override
    default String getDescription() {
        return "Search the web for information";
    }

    @Override
    default ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "query", AgentTool.PropertySchema.string("The search query"),
                        "num_results", AgentTool.PropertySchema.integer("Number of results (default 10)")
                ))
                .required(List.of("query"))
                .build();
    }

    @Override
    default CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();
        String query = args.get("query").toString();
        int numResults = (int) args.getOrDefault("num_results", 10);

        SearchOptions options = new SearchOptions(numResults, false, "en");

        return searchWithOptions(query, options)
                .thenApply(results -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Search results for: ").append(query).append("\n\n");
                    for (SearchResult result : results.results()) {
                        sb.append("Title: ").append(result.title()).append("\n");
                        sb.append("URL: ").append(result.url()).append("\n");
                        sb.append("Snippet: ").append(result.snippet()).append("\n\n");
                    }
                    return ToolResult.success(sb.toString());
                })
                .exceptionally(e -> ToolResult.failure(e.getMessage()));
    }

    /**
     * Search results.
     *
     * @param query the query
     * @param results the results
     * @param totalResults total count
     */
    record SearchResults(
            String query,
            List<SearchResult> results,
            int totalResults
    ) {
    }

    /**
     * Search result.
     *
     * @param title the title
     * @param url the URL
     * @param snippet the snippet
     * @param source the source
     */
    record SearchResult(
            String title,
            String url,
            String snippet,
            String source
    ) {
    }

    /**
     * Search options.
     *
     * @param numResults number of results
     * @param safeSearch whether to enable safe search
     * @param language the language
     */
    record SearchOptions(
            int numResults,
            boolean safeSearch,
            String language
    ) {
    }

    /**
     * Fetch result.
     *
     * @param url the URL
     * @param title the title
     * @param content the content
     * @param contentType the content type
     */
    record FetchResult(
            String url,
            String title,
            String content,
            String contentType
    ) {
    }
}
