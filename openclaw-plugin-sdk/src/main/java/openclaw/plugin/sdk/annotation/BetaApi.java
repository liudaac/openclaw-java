package openclaw.plugin.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a beta API that may change without notice.
 * <p>
 * APIs marked with this annotation are still evolving and may be
 * modified or removed in future versions. Use with caution.
 * <p>
 * Example usage:
 * <pre>
 * &#64;BetaApi(reason = "Subject to change based on user feedback")
 * public interface ExperimentalFeature {
 *     void doSomething();
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
public @interface BetaApi {
    /**
     * Reason why this API is in beta.
     *
     * @return the reason
     */
    String reason() default "";

    /**
     * Expected version when this API will become stable.
     *
     * @return the target version
     */
    String targetVersion() default "";
}
