package openclaw.tools.calendar;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.AgentTool.PropertySchema;
import openclaw.sdk.tool.AgentTool.ToolParameters;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Calendar Tool - 日历工具
 * 
 * 功能:
 * - 获取当前时间/日期
 * - 解析日期时间
 * - 格式化日期时间
 * - 计算时间差
 * - 时区转换
 * - 日历事件管理
 * 
 * 对应 Node.js: src/tools/calendar.ts
 */
@Component
public class CalendarTool implements AgentTool {
    
    private static final Logger logger = LoggerFactory.getLogger(CalendarTool.class);
    
    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public String getName() {
        return "calendar";
    }
    
    @Override
    public String getDescription() {
        return "Calendar operations including time parsing, formatting, timezone conversion, and event management";
    }
    
    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.ofEntries(
                        Map.entry("operation", PropertySchema.enum_("Calendar operation to perform", List.of("now", "parse", "format", "diff", "convert", "add", "subtract", "event"))),
                        Map.entry("datetime", PropertySchema.string("Date/time string to parse or format")),
                        Map.entry("format", PropertySchema.string("Output format pattern (e.g., 'yyyy-MM-dd HH:mm:ss')")),
                        Map.entry("inputFormat", PropertySchema.string("Input format pattern for parsing")),
                        Map.entry("fromZone", PropertySchema.string("Source timezone (e.g., 'UTC', 'America/New_York')")),
                        Map.entry("toZone", PropertySchema.string("Target timezone (e.g., 'Asia/Shanghai')")),
                        Map.entry("amount", PropertySchema.integer("Amount to add/subtract")),
                        Map.entry("unit", PropertySchema.enum_("Time unit", List.of("seconds", "minutes", "hours", "days", "weeks", "months", "years"))),
                        Map.entry("eventTitle", PropertySchema.string("Event title")),
                        Map.entry("eventDescription", PropertySchema.string("Event description")),
                        Map.entry("eventStart", PropertySchema.string("Event start time")),
                        Map.entry("eventEnd", PropertySchema.string("Event end time")),
                        Map.entry("eventAllDay", PropertySchema.boolean_("All day event")),
                        Map.entry("eventLocation", PropertySchema.string("Event location")),
                        Map.entry("eventReminder", PropertySchema.integer("Reminder minutes before event"))
                ))
                .required(List.of("operation"))
                .build();
    }

    // Deprecated: use getParameters() instead
    public String getSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["now", "parse", "format", "diff", "convert", "add", "subtract", "event"],
                  "description": "Calendar operation to perform"
                },
                "datetime": {
                  "type": "string",
                  "description": "Date/time string to parse or format"
                },
                "format": {
                  "type": "string",
                  "description": "Output format pattern (e.g., 'yyyy-MM-dd HH:mm:ss')"
                },
                "inputFormat": {
                  "type": "string",
                  "description": "Input format pattern for parsing"
                },
                "fromTimezone": {
                  "type": "string",
                  "description": "Source timezone (e.g., 'America/New_York')"
                },
                "toTimezone": {
                  "type": "string",
                  "description": "Target timezone (e.g., 'Asia/Shanghai')"
                },
                "amount": {
                  "type": "integer",
                  "description": "Amount to add/subtract"
                },
                "unit": {
                  "type": "string",
                  "enum": ["seconds", "minutes", "hours", "days", "weeks", "months", "years"],
                  "description": "Time unit"
                },
                "start": {
                  "type": "string",
                  "description": "Start datetime for diff operation"
                },
                "end": {
                  "type": "string",
                  "description": "End datetime for diff operation"
                },
                "event": {
                  "type": "object",
                  "properties": {
                    "title": {"type": "string"},
                    "description": {"type": "string"},
                    "start": {"type": "string"},
                    "end": {"type": "string"},
                    "timezone": {"type": "string"},
                    "location": {"type": "string"},
                    "attendees": {
                      "type": "array",
                      "items": {"type": "string"}
                    }
                  }
                }
              },
              "required": ["operation"]
            }
            """;
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> params = context.arguments();
                String operation = (String) params.get("operation");
                
                switch (operation) {
                    case "now":
                        return handleNow(params);
                    case "parse":
                        return handleParse(params);
                    case "format":
                        return handleFormat(params);
                    case "diff":
                        return handleDiff(params);
                    case "convert":
                        return handleConvert(params);
                    case "add":
                        return handleAdd(params);
                    case "subtract":
                        return handleSubtract(params);
                    case "event":
                        return handleEvent(params);
                    default:
                        return ToolResult.failure("Unknown operation: " + operation);
                }
                
            } catch (Exception e) {
                logger.error("Calendar operation failed", e);
                return ToolResult.failure("Calendar operation failed: " + e.getMessage());
            }
        });
    }
    
    private ToolResult handleNow(Map<String, Object> params) {
        String timezone = (String) params.get("toTimezone");
        ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        
        ZonedDateTime now = ZonedDateTime.now(zone);
        
        String format = (String) params.get("format");
        String formatted;
        if (format != null) {
            formatted = now.format(DateTimeFormatter.ofPattern(format));
        } else {
            formatted = now.format(ISO_FORMATTER);
        }
        
        return ToolResult.success("Current time", Map.ofEntries(
            Map.entry("datetime", formatted),
            Map.entry("timestamp", now.toInstant().toEpochMilli()),
            Map.entry("timezone", zone.getId()),
            Map.entry("iso", now.format(ISO_FORMATTER)),
            Map.entry("date", now.format(DATE_FORMATTER)),
            Map.entry("time", now.format(TIME_FORMATTER)),
            Map.entry("year", now.getYear()),
            Map.entry("month", now.getMonthValue()),
            Map.entry("day", now.getDayOfMonth()),
            Map.entry("hour", now.getHour()),
            Map.entry("minute", now.getMinute()),
            Map.entry("second", now.getSecond()),
            Map.entry("dayOfWeek", now.getDayOfWeek().toString()),
            Map.entry("dayOfYear", now.getDayOfYear())
        ));
    }
    
    private ToolResult handleParse(Map<String, Object> params) {
        String datetime = (String) params.get("datetime");
        String inputFormat = (String) params.get("inputFormat");
        String timezone = (String) params.get("fromTimezone");
        
        if (datetime == null) {
            return ToolResult.failure("datetime is required for parse operation");
        }
        
        try {
            LocalDateTime localDateTime;
            if (inputFormat != null) {
                localDateTime = LocalDateTime.parse(datetime, 
                    DateTimeFormatter.ofPattern(inputFormat));
            } else {
                localDateTime = LocalDateTime.parse(datetime, ISO_FORMATTER);
            }
            
            ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
            ZonedDateTime zonedDateTime = localDateTime.atZone(zone);
            
            return ToolResult.success("Calendar result", Map.of(
                "timestamp", zonedDateTime.toInstant().toEpochMilli(),
                "iso", zonedDateTime.format(ISO_FORMATTER),
                "date", zonedDateTime.format(DATE_FORMATTER),
                "time", zonedDateTime.format(TIME_FORMATTER),
                "timezone", zone.getId()
            ));
            
        } catch (DateTimeParseException e) {
            return ToolResult.failure("Failed to parse datetime: " + e.getMessage());
        }
    }
    
    private ToolResult handleFormat(Map<String, Object> params) {
        String datetime = (String) params.get("datetime");
        String format = (String) params.get("format");
        
        if (datetime == null || format == null) {
            return ToolResult.failure("datetime and format are required");
        }
        
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, ISO_FORMATTER);
            String formatted = zonedDateTime.format(DateTimeFormatter.ofPattern(format));
            
            return ToolResult.success("Calendar result", Map.of(
                "formatted", formatted,
                "original", datetime
            ));
            
        } catch (DateTimeParseException e) {
            return ToolResult.failure("Failed to format datetime: " + e.getMessage());
        }
    }
    
    private ToolResult handleDiff(Map<String, Object> params) {
        String start = (String) params.get("start");
        String end = (String) params.get("end");
        String unit = (String) params.getOrDefault("unit", "seconds");
        
        if (start == null || end == null) {
            return ToolResult.failure("start and end are required for diff operation");
        }
        
        try {
            ZonedDateTime startTime = ZonedDateTime.parse(start, ISO_FORMATTER);
            ZonedDateTime endTime = ZonedDateTime.parse(end, ISO_FORMATTER);
            
            Duration duration = Duration.between(startTime, endTime);
            
            long diffValue;
            switch (unit) {
                case "seconds":
                    diffValue = duration.getSeconds();
                    break;
                case "minutes":
                    diffValue = duration.toMinutes();
                    break;
                case "hours":
                    diffValue = duration.toHours();
                    break;
                case "days":
                    diffValue = duration.toDays();
                    break;
                default:
                    diffValue = duration.getSeconds();
            }
            
            return ToolResult.success("Calendar result", Map.of(
                "diff", diffValue,
                "unit", unit,
                "inSeconds", duration.getSeconds(),
                "inMinutes", duration.toMinutes(),
                "inHours", duration.toHours(),
                "inDays", duration.toDays(),
                "isNegative", duration.isNegative()
            ));
            
        } catch (DateTimeParseException e) {
            return ToolResult.failure("Failed to parse datetime: " + e.getMessage());
        }
    }
    
    private ToolResult handleConvert(Map<String, Object> params) {
        String datetime = (String) params.get("datetime");
        String fromTimezone = (String) params.get("fromTimezone");
        String toTimezone = (String) params.get("toTimezone");
        
        if (datetime == null || fromTimezone == null || toTimezone == null) {
            return ToolResult.failure("datetime, fromTimezone, and toTimezone are required");
        }
        
        try {
            ZonedDateTime sourceTime = ZonedDateTime.parse(datetime, ISO_FORMATTER);
            ZonedDateTime targetTime = sourceTime.withZoneSameInstant(ZoneId.of(toTimezone));
            
            return ToolResult.success("Calendar result", Map.of(
                "original", datetime,
                "originalTimezone", fromTimezone,
                "converted", targetTime.format(ISO_FORMATTER),
                "targetTimezone", toTimezone,
                "date", targetTime.format(DATE_FORMATTER),
                "time", targetTime.format(TIME_FORMATTER)
            ));
            
        } catch (DateTimeParseException e) {
            return ToolResult.failure("Failed to parse datetime: " + e.getMessage());
        }
    }
    
    private ToolResult handleAdd(Map<String, Object> params) {
        return handleAddSubtract(params, true);
    }
    
    private ToolResult handleSubtract(Map<String, Object> params) {
        return handleAddSubtract(params, false);
    }
    
    private ToolResult handleAddSubtract(Map<String, Object> params, boolean isAdd) {
        String datetime = (String) params.get("datetime");
        Integer amount = (Integer) params.get("amount");
        String unit = (String) params.get("unit");
        
        if (datetime == null || amount == null || unit == null) {
            return ToolResult.failure("datetime, amount, and unit are required");
        }
        
        if (!isAdd) {
            amount = -amount;
        }
        
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, ISO_FORMATTER);
            
            switch (unit) {
                case "seconds":
                    zonedDateTime = zonedDateTime.plusSeconds(amount);
                    break;
                case "minutes":
                    zonedDateTime = zonedDateTime.plusMinutes(amount);
                    break;
                case "hours":
                    zonedDateTime = zonedDateTime.plusHours(amount);
                    break;
                case "days":
                    zonedDateTime = zonedDateTime.plusDays(amount);
                    break;
                case "weeks":
                    zonedDateTime = zonedDateTime.plusWeeks(amount);
                    break;
                case "months":
                    zonedDateTime = zonedDateTime.plusMonths(amount);
                    break;
                case "years":
                    zonedDateTime = zonedDateTime.plusYears(amount);
                    break;
            }
            
            return ToolResult.success("Calendar result", Map.of(
                "original", datetime,
                "result", zonedDateTime.format(ISO_FORMATTER),
                "operation", isAdd ? "add" : "subtract",
                "amount", Math.abs(amount),
                "unit", unit
            ));
            
        } catch (DateTimeParseException e) {
            return ToolResult.failure("Failed to parse datetime: " + e.getMessage());
        }
    }
    
    private ToolResult handleEvent(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) params.get("event");
        
        if (event == null) {
            return ToolResult.failure("event is required for event operation");
        }
        
        String title = (String) event.get("title");
        String description = (String) event.get("description");
        String start = (String) event.get("start");
        String end = (String) event.get("end");
        String timezone = (String) event.get("timezone");
        String location = (String) event.get("location");
        @SuppressWarnings("unchecked")
        List<String> attendees = (List<String>) event.get("attendees");
        
        // 生成 iCalendar 格式
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\n");
        ical.append("VERSION:2.0\n");
        ical.append("PRODID:-//OpenClaw//Calendar//EN\n");
        ical.append("BEGIN:VEVENT\n");
        ical.append("UID:").append(UUID.randomUUID()).append("@openclaw\n");
        ical.append("SUMMARY:").append(escapeIcalText(title)).append("\n");
        
        if (description != null) {
            ical.append("DESCRIPTION:").append(escapeIcalText(description)).append("\n");
        }
        
        if (start != null) {
            ZonedDateTime startTime = ZonedDateTime.parse(start, ISO_FORMATTER);
            ical.append("DTSTART:").append(startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))).append("\n");
        }
        
        if (end != null) {
            ZonedDateTime endTime = ZonedDateTime.parse(end, ISO_FORMATTER);
            ical.append("DTEND:").append(endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))).append("\n");
        }
        
        if (location != null) {
            ical.append("LOCATION:").append(escapeIcalText(location)).append("\n");
        }
        
        if (attendees != null) {
            for (String attendee : attendees) {
                ical.append("ATTENDEE:mailto:").append(attendee).append("\n");
            }
        }
        
        ical.append("END:VEVENT\n");
        ical.append("END:VCALENDAR");
        
        return ToolResult.success("Calendar result", Map.of(
            "ical", ical.toString(),
            "title", title,
            "start", start,
            "end", end,
            "timezone", timezone != null ? timezone : "UTC",
            "attendees", attendees != null ? attendees.size() : 0
        ));
    }
    
    private String escapeIcalText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace("\n", "\\n");
    }
}
