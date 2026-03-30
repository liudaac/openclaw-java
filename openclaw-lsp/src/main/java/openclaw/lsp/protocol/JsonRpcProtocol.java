package openclaw.lsp.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON-RPC Protocol encoder/decoder.
 * Handles Content-Length header framing.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public final class JsonRpcProtocol {

    private static final Logger logger = LoggerFactory.getLogger(JsonRpcProtocol.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile("Content-Length:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private JsonRpcProtocol() {
        // Utility class
    }

    /**
     * Encode a message with Content-Length header.
     *
     * @param message the message to encode
     * @return the encoded message
     */
    public static String encode(JsonRpcMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return "Content-Length: " + bytes.length + "\r\n\r\n" + json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to encode JSON-RPC message", e);
        }
    }

    /**
     * Decode messages from buffer.
     *
     * @param buffer the buffer containing received data
     * @return decode result with messages and remaining buffer
     */
    public static DecodeResult decode(String buffer) {
        List<JsonRpcMessage> messages = new ArrayList<>();
        String remaining = buffer;

        while (true) {
            // Find header end
            int headerEnd = remaining.indexOf("\r\n\r\n");
            if (headerEnd == -1) {
                // Incomplete header
                break;
            }

            // Parse Content-Length
            String header = remaining.substring(0, headerEnd);
            Matcher matcher = CONTENT_LENGTH_PATTERN.matcher(header);
            if (!matcher.find()) {
                // Invalid header, skip
                logger.warn("Invalid JSON-RPC header: {}", header);
                remaining = remaining.substring(headerEnd + 4);
                continue;
            }

            int contentLength = Integer.parseInt(matcher.group(1));
            int bodyStart = headerEnd + 4;
            int bodyEnd = bodyStart + contentLength;

            // Check if we have enough data
            if (remaining.length() < bodyEnd) {
                // Incomplete body
                break;
            }

            // Parse body
            String body = remaining.substring(bodyStart, bodyEnd);
            try {
                JsonRpcMessage message = objectMapper.readValue(body, JsonRpcMessage.class);
                messages.add(message);
                logger.debug("Decoded JSON-RPC message: id={}, method={}",
                        message.getId(), message.getMethod());
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse JSON-RPC body: {}", body, e);
            }

            // Move to next message
            remaining = remaining.substring(bodyEnd);
        }

        return new DecodeResult(messages, remaining);
    }

    /**
     * Decode result containing parsed messages and remaining buffer.
     */
    public static class DecodeResult {
        private final List<JsonRpcMessage> messages;
        private final String remaining;

        public DecodeResult(List<JsonRpcMessage> messages, String remaining) {
            this.messages = messages;
            this.remaining = remaining;
        }

        public List<JsonRpcMessage> getMessages() {
            return messages;
        }

        public String getRemaining() {
            return remaining;
        }

        public boolean hasMessages() {
            return !messages.isEmpty();
        }
    }
}
