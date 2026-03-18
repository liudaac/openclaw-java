package openclaw.plugin.sdk.internal;

import openclaw.plugin.sdk.annotation.InternalApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility class for reflection operations.
 * <p>
 * This is an internal class and should not be used directly by plugins.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@InternalApi(reason = "Internal reflection utilities")
public final class ReflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // Utility class
    }

    /**
     * Create an instance of the given class using the no-arg constructor.
     *
     * @param <T> the type
     * @param clazz the class
     * @return the instance
     * @throws RuntimeException if creation fails
     */
    public static <T> T createInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No no-arg constructor found for " + clazz.getName(), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    /**
     * Create an instance using the specified constructor arguments.
     *
     * @param <T> the type
     * @param clazz the class
     * @param argTypes the constructor argument types
     * @param args the constructor arguments
     * @return the instance
     * @throws RuntimeException if creation fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T createInstance(Class<T> clazz, Class<?>[] argTypes, Object[] args) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Constructor not found for " + clazz.getName(), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    /**
     * Get the simple name of a class without package.
     *
     * @param clazz the class
     * @return the simple name
     */
    public static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * Check if a class is assignable from another class.
     *
     * @param target the target type
     * @param source the source type
     * @return true if assignable
     */
    public static boolean isAssignableFrom(Class<?> target, Class<?> source) {
        return target.isAssignableFrom(source);
    }
}
