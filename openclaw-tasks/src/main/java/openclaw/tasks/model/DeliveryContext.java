package openclaw.tasks.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Delivery context for flow/task notifications.
 * Mirrors the TypeScript type: DeliveryContext
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryContext {
    private String channel;
    private String to;
    private String accountId;
    private String threadId;
}
