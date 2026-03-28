package openclaw.gateway.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require specific operator scopes.
 *
 * <p>Usage:
 * <pre>
 *   @RequiresScope(OperatorScopes.READ_SCOPE)
 *   public ResponseEntity&lt;?&gt; getStatus() { ... }
 *
 *   @RequiresScope({OperatorScopes.READ_SCOPE, OperatorScopes.WRITE_SCOPE})
 *   public ResponseEntity&lt;?&gt; complexOperation() { ... }
 * </pre>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresScope {
    /**
     * The required scopes.
     * If multiple scopes are specified, any one of them grants access.
     *
     * @return the required scopes
     */
    String[] value() default {};

    /**
     * Whether all specified scopes are required (AND) vs any scope (OR).
     * Default is OR (false).
     *
     * @return true if all scopes required
     */
    boolean allRequired() default false;

    /**
     * Optional description of why this scope is required.
     *
     * @return the description
     */
    String description() default "";
}
