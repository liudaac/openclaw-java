package openclaw.tools.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.plugin.sdk.tool.Tool;
import openclaw.plugin.sdk.tool.ToolContext;
import openclaw.plugin.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Finance Tool - 金融数据查询工具
 * 
 * 功能:
 * - 获取股票价格
 * - 获取加密货币价格
 * - 获取汇率
 * - 获取市场指数
 * 
 * 使用 Yahoo Finance API (免费，无需 API Key)
 * 
 * 对应 Node.js: src/tools/finance.ts
 */
@Component
public class FinanceTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(FinanceTool.class);
    
    // Yahoo Finance API endpoints
    private static final String YAHOO_QUOTE_URL = "https://query1.finance.yahoo.com/v8/finance/chart";
    private static final String YAHOO_SEARCH_URL = "https://query2.finance.yahoo.com/v1/finance/search";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public FinanceTool() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "finance";
    }
    
    @Override
    public String getDescription() {
        return "Get stock prices, cryptocurrency rates, forex rates, and market indices from Yahoo Finance";
    }
    
    @Override
    public String getSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["quote", "search", "crypto", "forex", "history"],
                  "description": "Finance operation",
                  "default": "quote"
                },
                "symbol": {
                  "type": "string",
                  "description": "Stock symbol (e.g., 'AAPL', 'MSFT', 'TSLA')"
                },
                "query": {
                  "type": "string",
                  "description": "Search query for search operation"
                },
                "from": {
                  "type": "string",
                  "description": "From currency for forex (e.g., 'USD')"
                },
                "to": {
                  "type": "string",
                  "description": "To currency for forex (e.g., 'CNY')"
                },
                "period": {
                  "type": "string",
                  "enum": ["1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max"],
                  "description": "Time period for history",
                  "default": "1d"
                },
                "interval": {
                  "type": "string",
                  "enum": ["1m", "2m", "5m", "15m", "30m", "60m", "90m", "1h", "1d", "5d", "1wk", "1mo", "3mo"],
                  "description": "Data interval",
                  "default": "1d"
                }
              },
              "required": ["operation"]
            }
            """;
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(Map<String, Object> params, ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = (String) params.get("operation");
                
                switch (operation) {
                    case "quote":
                        return handleQuote(params);
                    case "search":
                        return handleSearch(params);
                    case "crypto":
                        return handleCrypto(params);
                    case "forex":
                        return handleForex(params);
                    case "history":
                        return handleHistory(params);
                    default:
                        return ToolResult.error("Unknown operation: " + operation);
                }
                
            } catch (Exception e) {
                logger.error("Finance query failed", e);
                return ToolResult.error("Finance query failed: " + e.getMessage());
            }
        });
    }
    
    private ToolResult handleQuote(Map<String, Object> params) {
        String symbol = (String) params.get("symbol");
        if (symbol == null) {
            return ToolResult.error("symbol is required for quote operation");
        }
        
        try {
            String url = YAHOO_QUOTE_URL + "/" + symbol + "?interval=1d&range=1d";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.error("Finance API error: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("chart").path("result").get(0);
            
            if (result == null) {
                return ToolResult.error("No data found for symbol: " + symbol);
            }
            
            JsonNode meta = result.path("meta");
            JsonNode quote = result.path("indicators").path("quote").get(0);
            
            String currency = meta.path("currency").asText();
            String exchange = meta.path("exchangeName").asText();
            String shortName = meta.path("shortName").asText();
            String longName = meta.path("longName").asText();
            String symbolName = meta.path("symbol").asText();
            
            double regularMarketPrice = meta.path("regularMarketPrice").asDouble();
            double previousClose = meta.path("previousClose").asDouble();
            double change = regularMarketPrice - previousClose;
            double changePercent = previousClose > 0 ? (change / previousClose) * 100 : 0;
            
            // 获取最新数据
            JsonNode closeArray = quote.path("close");
            JsonNode highArray = quote.path("high");
            JsonNode lowArray = quote.path("low");
            JsonNode volumeArray = quote.path("volume");
            
            double latestClose = getLastValidValue(closeArray);
            double dayHigh = getLastValidValue(highArray);
            double dayLow = getLastValidValue(lowArray);
            long volume = getLastValidValue(volumeArray).longValue();
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("symbol", symbolName);
            resultMap.put("name", longName.isEmpty() ? shortName : longName);
            resultMap.put("currency", currency);
            resultMap.put("exchange", exchange);
            resultMap.put("price", regularMarketPrice);
            resultMap.put("previousClose", previousClose);
            resultMap.put("change", change);
            resultMap.put("changePercent", changePercent);
            resultMap.put("dayHigh", dayHigh);
            resultMap.put("dayLow", dayLow);
            resultMap.put("volume", volume);
            resultMap.put("timestamp", System.currentTimeMillis());
            
            return ToolResult.success(resultMap);
            
        } catch (Exception e) {
            logger.error("Failed to get quote for: " + symbol, e);
            return ToolResult.error("Failed to get quote: " + e.getMessage());
        }
    }
    
    private ToolResult handleSearch(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null) {
            return ToolResult.error("query is required for search operation");
        }
        
        try {
            String url = YAHOO_SEARCH_URL + "?q=" + encodeUrl(query) + "&quotesCount=10&newsCount=0";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.error("Search API error: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode quotes = root.path("quotes");
            
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode quote : quotes) {
                Map<String, Object> item = new HashMap<>();
                item.put("symbol", quote.path("symbol").asText());
                item.put("name", quote.path("shortname").asText());
                item.put("longName", quote.path("longname").asText());
                item.put("exchange", quote.path("exchange").asText());
                item.put("type", quote.path("quoteType").asText());
                item.put("sector", quote.path("sector").asText());
                item.put("industry", quote.path("industry").asText());
                results.add(item);
            }
            
            return ToolResult.success(Map.of(
                "query", query,
                "count", results.size(),
                "results", results
            ));
            
        } catch (Exception e) {
            logger.error("Search failed for: " + query, e);
            return ToolResult.error("Search failed: " + e.getMessage());
        }
    }
    
    private ToolResult handleCrypto(Map<String, Object> params) {
        String symbol = (String) params.get("symbol");
        if (symbol == null) {
            return ToolResult.error("symbol is required for crypto operation");
        }
        
        // 加密货币符号格式化 (例如 BTC -> BTC-USD)
        String cryptoSymbol = symbol.toUpperCase();
        if (!cryptoSymbol.contains("-")) {
            cryptoSymbol = cryptoSymbol + "-USD";
        }
        
        // 复用 quote 逻辑
        params.put("symbol", cryptoSymbol);
        return handleQuote(params);
    }
    
    private ToolResult handleForex(Map<String, Object> params) {
        String from = (String) params.get("from");
        String to = (String) params.get("to");
        
        if (from == null || to == null) {
            return ToolResult.error("from and to are required for forex operation");
        }
        
        // 外汇符号格式化 (例如 USD -> CNY = USDCNY=X)
        String forexSymbol = from.toUpperCase() + to.toUpperCase() + "=X";
        
        // 复用 quote 逻辑
        params.put("symbol", forexSymbol);
        return handleQuote(params);
    }
    
    private ToolResult handleHistory(Map<String, Object> params) {
        String symbol = (String) params.get("symbol");
        String period = (String) params.getOrDefault("period", "1mo");
        String interval = (String) params.getOrDefault("interval", "1d");
        
        if (symbol == null) {
            return ToolResult.error("symbol is required for history operation");
        }
        
        try {
            String url = YAHOO_QUOTE_URL + "/" + symbol + 
                "?interval=" + interval + "&range=" + period;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.error("History API error: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("chart").path("result").get(0);
            
            if (result == null) {
                return ToolResult.error("No history data found for symbol: " + symbol);
            }
            
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);
            JsonNode opens = quote.path("open");
            JsonNode highs = quote.path("high");
            JsonNode lows = quote.path("low");
            JsonNode closes = quote.path("close");
            JsonNode volumes = quote.path("volume");
            
            List<Map<String, Object>> history = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                if (closes.get(i).isNull()) continue;
                
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("timestamp", timestamps.get(i).asLong() * 1000);
                dataPoint.put("open", opens.get(i).asDouble());
                dataPoint.put("high", highs.get(i).asDouble());
                dataPoint.put("low", lows.get(i).asDouble());
                dataPoint.put("close", closes.get(i).asDouble());
                dataPoint.put("volume", volumes.get(i).asLong());
                history.add(dataPoint);
            }
            
            return ToolResult.success(Map.of(
                "symbol", symbol,
                "period", period,
                "interval", interval,
                "count", history.size(),
                "history", history
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get history for: " + symbol, e);
            return ToolResult.error("Failed to get history: " + e.getMessage());
        }
    }
    
    private Double getLastValidValue(JsonNode array) {
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonNode value = array.get(i);
            if (!value.isNull()) {
                return value.asDouble();
            }
        }
        return 0.0;
    }
    
    private String encodeUrl(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
