package openclaw.plugin.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a deprecated API with additional metadata.
 * <p>
 * This annotation provides more detailed information about deprecated
 * APIs, including replacement suggestions and removal timeline.
 * <p>
 * Example usage:
 * <pre>
 * &#64;Deprecated(since = "2026.3.0", forRemoval = true)
 * &#64;DeprecatedApi(
 *     replacement = "NewApi.class",
 *     removalVersion = "2026.6.0",
 *     migrationGuide = "https://docs.openclaw.ai/migration"
 * )
 * public interface OldApi {
 *     // ...
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
public @interface DeprecatedApi {
    /**
     * The replacement API to use instead.
     *
     * @return the replacement class or method
     */
    String replacement() default "";

    /**
     * The version when this API will be removed.
     *
     * @return the removal version
     */
    String removalVersion() default "";

    /**
     * URL to migration guide.
     *
     * @return the migration guide URL
     */
    String migrationGuide() default "";

    /**
     * Reason for deprecation.
     *
     * @return the reason
     */
    String reason() default "";
}
