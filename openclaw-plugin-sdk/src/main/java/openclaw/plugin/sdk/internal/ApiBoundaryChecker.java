package openclaw.plugin.sdk.internal;

import openclaw.plugin.sdk.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/**
 * Checks API boundaries at runtime.
 * <p>
 * This is an internal class used to validate that plugins are only
 * using public APIs.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@InternalApi(reason = "Internal API boundary checking")
public final class ApiBoundaryChecker {

    private static final Logger logger = LoggerFactory.getLogger(ApiBoundaryChecker.class);

    private static final Set<String> ALLOWED_PACKAGES = Set.of(
            "openclaw.plugin.sdk.api",
            "openclaw.plugin.sdk.spi",
            "openclaw.plugin.sdk.util",
            "openclaw.plugin.sdk.websearch",
            "openclaw.plugin.sdk.annotation"
    );

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "java.",
            "javax.",
            "com.fasterxml.jackson",
            "org.slf4j"
    );

    private ApiBoundaryChecker() {
        // Utility class
    }

    /**
     * Check if a class is part of the public API.
     *
     * @param clazz the class to check
     * @return true if public API
     */
    public static boolean isPublicApi(Class<?> clazz) {
        if (clazz == null) return true;

        // Check annotations
        if (clazz.isAnnotationPresent(PublicApi.class)) {
            return true;
        }
        if (clazz.isAnnotationPresent(SpiApi.class)) {
            return true;
        }
        if (clazz.isAnnotationPresent(BetaApi.class)) {
            return true;
        }

        // Check if internal
        if (clazz.isAnnotationPresent(InternalApi.class)) {
            return false;
        }

        // Check package
        String packageName = clazz.getPackageName();
        if (ALLOWED_PACKAGES.contains(packageName)) {
            return true;
        }

        // Check allowed prefixes
        for (String prefix : ALLOWED_PREFIXES) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a method is part of the public API.
     *
     * @param method the method to check
     * @return true if public API
     */
    public static boolean isPublicApi(Method method) {
        if (method == null) return true;

        // Check method annotations
        if (method.isAnnotationPresent(PublicApi.class)) {
            return true;
        }
        if (method.isAnnotationPresent(BetaApi.class)) {
            return true;
        }

        // Check if internal
        if (method.isAnnotationPresent(InternalApi.class)) {
            return false;
        }

        // Check declaring class
        return isPublicApi(method.getDeclaringClass());
    }

    /**
     * Check if a field is part of the public API.
     *
     * @param field the field to check
     * @return true if public API
     */
    public static boolean isPublicApi(Field field) {
        if (field == null) return true;

        if (field.isAnnotationPresent(PublicApi.class)) {
            return true;
        }
        if (field.isAnnotationPresent(BetaApi.class)) {
            return true;
        }

        if (field.isAnnotationPresent(InternalApi.class)) {
            return false;
        }

        return isPublicApi(field.getDeclaringClass());
    }

    /**
     * Validate that a class only uses public APIs.
     *
     * @param clazz the class to validate
     * @return list of violations
     */
    public static List<String> validateClass(Class<?> clazz) {
        List<String> violations = new ArrayList<>();

        // Check fields
        for (Field field : clazz.getDeclaredFields()) {
            if (!isPublicApi(field.getType())) {
                violations.add("Field " + field.getName() + " uses internal type: " + field.getType().getName());
            }
        }

        // Check methods
        for (Method method : clazz.getDeclaredMethods()) {
            // Check return type
            if (!isPublicApi(method.getReturnType())) {
                violations.add("Method " + method.getName() + " returns internal type: " + method.getReturnType().getName());
            }

            // Check parameter types
            for (Class<?> paramType : method.getParameterTypes()) {
                if (!isPublicApi(paramType)) {
                    violations.add("Method " + method.getName() + " uses internal parameter type: " + paramType.getName());
                }
            }
        }

        return violations;
    }

    /**
     * Log a warning if an internal API is accessed.
     *
     * @param clazz the class being accessed
     * @param context the context for the warning
     */
    public static void warnIfInternal(Class<?> clazz, String context) {
        if (!isPublicApi(clazz)) {
            logger.warn("Plugin is accessing internal API: {} (context: {}). " +
                    "This may break in future versions.", clazz.getName(), context);
        }
    }

    /**
     * Check if a package is internal.
     *
     * @param packageName the package name
     * @return true if internal
     */
    public static boolean isInternalPackage(String packageName) {
        return packageName.contains(".internal.") || packageName.endsWith(".internal");
    }
}
