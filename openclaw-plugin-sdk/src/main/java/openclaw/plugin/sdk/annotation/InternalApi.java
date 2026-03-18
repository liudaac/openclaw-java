package openclaw.plugin.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks an internal API that should not be used by plugins.
 * <p>
 * APIs marked with this annotation are implementation details and
 * may change or be removed at any time without notice. Using these
 * APIs in plugins may cause breakage in future versions.
 * <p>
 * Example usage:
 * <pre>
 * &#64;InternalApi
 * class PluginRegistry {
 *     void register(Plugin plugin) { ... }
 * }
 * </pre>
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Documented
public @interface InternalApi {
    /**
     * Reason why this API is internal.
     *
     * @return the reason
     */
    String reason() default "";

    /**
     * Whether this internal API might become public in the future.
     *
     * @return true if potentially public
     */
    boolean potentiallyPublic() default false;
}
