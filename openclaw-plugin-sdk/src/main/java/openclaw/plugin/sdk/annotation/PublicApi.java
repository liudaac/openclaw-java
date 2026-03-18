package openclaw.plugin.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a public, stable API that is safe to use in plugins.
 * <p>
 * APIs marked with this annotation promise backward compatibility
 * and will not change in a breaking way within major versions.
 * <p>
 * Example usage:
 * <pre>
 * &#64;PublicApi(since = "2026.3.0", stability = "stable")
 * public interface AgentTool {
 *     String getName();
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
public @interface PublicApi {
    /**
     * The version when this API was first introduced.
     *
     * @return the version string
     */
    String since() default "";

    /**
     * The stability level of this API.
     *
     * @return "stable", "experimental", or "deprecated"
     */
    String stability() default "stable";

    /**
     * Optional description of the API.
     *
     * @return the description
     */
    String description() default "";
}
