package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * JSON-RPC 2.0 Message.
 * Base class for all LSP messages.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private JsonRpcError error;

    public JsonRpcMessage() {
    }

    // Request constructor
    public JsonRpcMessage(Integer id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // Response constructor
    public JsonRpcMessage(Integer id, Object result) {
        this.id = id;
        this.result = result;
    }

    // Error response constructor
    public JsonRpcMessage(Integer id, JsonRpcError error) {
        this.id = id;
        this.error = error;
    }

    // Notification constructor
    public JsonRpcMessage(String method, Object params) {
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }

    public boolean isRequest() {
        return id != null && method != null;
    }

    public boolean isResponse() {
        return id != null && method == null;
    }

    public boolean isNotification() {
        return id == null && method != null;
    }

    public boolean isError() {
        return error != null;
    }

    @Override
    public String toString() {
        return "JsonRpcMessage{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", id=" + id +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", result=" + result +
                ", error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonRpcMessage that = (JsonRpcMessage) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, method);
    }
}
