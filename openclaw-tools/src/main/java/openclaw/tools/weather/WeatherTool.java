package openclaw.tools.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.AgentTool.PropertySchema;
import openclaw.sdk.tool.AgentTool.ToolParameters;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Weather Tool - 天气查询工具
 * 
 * 功能:
 * - 获取当前天气
 * - 获取天气预报
 * - 支持多城市
 * - 支持多语言
 * 
 * 使用 Open-Meteo API (免费，无需 API Key)
 * 
 * 对应 Node.js: src/tools/weather.ts
 */
@Component
public class WeatherTool implements AgentTool {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);
    
    private static final String OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1";
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WeatherTool() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "Get current weather and forecast for any location using Open-Meteo API";
    }
    
    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "location", PropertySchema.string("City name or location (e.g., 'Beijing', 'New York', 'London')"),
                        "operation", PropertySchema.enum_("Weather operation", List.of("current", "forecast")),
                        "days", PropertySchema.integer("Number of forecast days (1-14)"),
                        "language", PropertySchema.string("Language for weather description (default: en)"),
                        "units", PropertySchema.enum_("Temperature units", List.of("metric", "imperial"))
                ))
                .required(List.of("location"))
                .build();
    }

    // Deprecated: use getParameters() instead
    public String getSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "location": {
                  "type": "string",
                  "description": "City name or location (e.g., 'Beijing', 'New York', 'London')"
                },
                "operation": {
                  "type": "string",
                  "enum": ["current", "forecast"],
                  "description": "Weather operation",
                  "default": "current"
                },
                "days": {
                  "type": "integer",
                  "description": "Number of forecast days (1-14)",
                  "default": 3
                },
                "language": {
                  "type": "string",
                  "description": "Language for weather descriptions",
                  "default": "en"
                },
                "units": {
                  "type": "string",
                  "enum": ["metric", "imperial"],
                  "description": "Temperature units",
                  "default": "metric"
                }
              },
              "required": ["location"]
            }
            """;
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> params = context.arguments();
                String location = (String) params.get("location");
                String operation = (String) params.getOrDefault("operation", "current");
                int days = (int) params.getOrDefault("days", 3);
                String language = (String) params.getOrDefault("language", "en");
                String units = (String) params.getOrDefault("units", "metric");
                
                // 地理编码：获取城市坐标
                GeocodingResult geoResult = geocodeLocation(location).join();
                if (geoResult == null) {
                    return ToolResult.failure("Location not found: " + location);
                }
                
                // 获取天气数据
                if ("current".equals(operation)) {
                    return getCurrentWeather(geoResult, language, units);
                } else if ("forecast".equals(operation)) {
                    return getForecast(geoResult, days, language, units);
                } else {
                    return ToolResult.failure("Unknown operation: " + operation);
                }
                
            } catch (Exception e) {
                logger.error("Weather query failed", e);
                return ToolResult.failure("Weather query failed: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<GeocodingResult> geocodeLocation(String location) {
        String url = GEOCODING_URL + "?name=" + encodeUrl(location) + "&count=1";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Geocoding failed: " + response.statusCode());
                }
                
                try {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode results = root.path("results");
                    
                    if (results.isEmpty()) {
                        return null;
                    }
                    
                    JsonNode first = results.get(0);
                    return new GeocodingResult(
                        first.path("name").asText(),
                        first.path("latitude").asDouble(),
                        first.path("longitude").asDouble(),
                        first.path("country").asText(),
                        first.path("admin1").asText()
                    );
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse geocoding response", e);
                }
            });
    }
    
    private ToolResult getCurrentWeather(GeocodingResult geo, String language, String units) {
        try {
            String url = buildWeatherUrl(geo.getLatitude(), geo.getLongitude(), 
                language, units, false);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.failure("Weather API error: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current_weather");
            
            double temperature = current.path("temperature").asDouble();
            double windSpeed = current.path("windspeed").asDouble();
            int windDirection = current.path("winddirection").asInt();
            int weatherCode = current.path("weathercode").asInt();
            String time = current.path("time").asText();
            
            String weatherDescription = getWeatherDescription(weatherCode);
            String windDirectionText = getWindDirection(windDirection);
            
            Map<String, Object> result = new HashMap<>();
            result.put("location", geo.getName());
            result.put("country", geo.getCountry());
            result.put("region", geo.getRegion());
            result.put("latitude", geo.getLatitude());
            result.put("longitude", geo.getLongitude());
            result.put("temperature", temperature);
            result.put("temperatureUnit", "celsius".equals(getTemperatureUnit(units)) ? "°C" : "°F");
            result.put("weatherCode", weatherCode);
            result.put("weatherDescription", weatherDescription);
            result.put("windSpeed", windSpeed);
            result.put("windDirection", windDirection);
            result.put("windDirectionText", windDirectionText);
            result.put("time", time);
            result.put("isDay", current.path("is_day").asInt() == 1);
            
            return ToolResult.success("Weather data retrieved", result);
            
        } catch (Exception e) {
            logger.error("Failed to get current weather", e);
            return ToolResult.failure("Failed to get weather: " + e.getMessage());
        }
    }
    
    private ToolResult getForecast(GeocodingResult geo, int days, String language, String units) {
        try {
            String url = buildWeatherUrl(geo.getLatitude(), geo.getLongitude(), 
                language, units, true) + "&forecast_days=" + days;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.failure("Weather API error: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode daily = root.path("daily");
            
            List<Map<String, Object>> forecastList = new ArrayList<>();
            
            JsonNode timeArray = daily.path("time");
            JsonNode maxTempArray = daily.path("temperature_2m_max");
            JsonNode minTempArray = daily.path("temperature_2m_min");
            JsonNode weatherCodeArray = daily.path("weathercode");
            
            for (int i = 0; i < timeArray.size(); i++) {
                Map<String, Object> dayForecast = new HashMap<>();
                dayForecast.put("date", timeArray.get(i).asText());
                dayForecast.put("maxTemp", maxTempArray.get(i).asDouble());
                dayForecast.put("minTemp", minTempArray.get(i).asDouble());
                dayForecast.put("weatherCode", weatherCodeArray.get(i).asInt());
                dayForecast.put("weatherDescription", 
                    getWeatherDescription(weatherCodeArray.get(i).asInt()));
                forecastList.add(dayForecast);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("location", geo.getName());
            result.put("country", geo.getCountry());
            result.put("region", geo.getRegion());
            result.put("latitude", geo.getLatitude());
            result.put("longitude", geo.getLongitude());
            result.put("temperatureUnit", "celsius".equals(getTemperatureUnit(units)) ? "°C" : "°F");
            result.put("days", days);
            result.put("forecast", forecastList);
            
            return ToolResult.success("Weather data retrieved", result);
            
        } catch (Exception e) {
            logger.error("Failed to get forecast", e);
            return ToolResult.failure("Failed to get forecast: " + e.getMessage());
        }
    }
    
    private String buildWeatherUrl(double lat, double lon, String language, 
                                   String units, boolean includeDaily) {
        StringBuilder url = new StringBuilder(OPEN_METEO_BASE_URL);
        url.append("/forecast?latitude=").append(lat);
        url.append("&longitude=").append(lon);
        url.append("&current_weather=true");
        url.append("&timezone=auto");
        
        if ("celsius".equals(getTemperatureUnit(units))) {
            url.append("&temperature_unit=celsius");
        } else {
            url.append("&temperature_unit=fahrenheit");
        }
        
        if (includeDaily) {
            url.append("&daily=temperature_2m_max,temperature_2m_min,weathercode");
        }
        
        return url.toString();
    }
    
    private String getTemperatureUnit(String units) {
        return "imperial".equals(units) ? "fahrenheit" : "celsius";
    }
    
    private String encodeUrl(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private String getWeatherDescription(int code) {
        // WMO Weather interpretation codes (WW)
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow fall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }
    
    private String getWindDirection(int degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                              "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }
    
    // Inner classes
    
    private static class GeocodingResult {
        private final String name;
        private final double latitude;
        private final double longitude;
        private final String country;
        private final String region;
        
        public GeocodingResult(String name, double latitude, double longitude,
                              String country, String region) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.country = country;
            this.region = region;
        }
        
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getCountry() { return country; }
        public String getRegion() { return region; }
    }
}
