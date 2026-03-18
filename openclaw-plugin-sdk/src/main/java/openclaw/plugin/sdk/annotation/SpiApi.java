package openclaw.plugin.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a Service Provider Interface (SPI) API.
 * <p>
 * APIs marked with this annotation are intended to be implemented
 * by service providers (plugins). These are not called by users
 * directly, but are implemented by plugin authors.
 * <p>
 * Example usage:
 * <pre>
 * &#64;SpiApi
 * public interface WebSearchProvider {
 *     String getId();
 *     WebSearchToolDefinition createTool(WebSearchContext ctx);
 * }
 * </pre>
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface SpiApi {
    /**
     * The service type this SPI provides.
     *
     * @return the service type
     */
    String serviceType() default "";

    /**
     * Whether multiple implementations are allowed.
     *
     * @return true if multiple implementations allowed
     */
    boolean multipleImplementations() default true;
}
