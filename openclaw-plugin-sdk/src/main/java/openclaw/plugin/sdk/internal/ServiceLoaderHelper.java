package openclaw.plugin.sdk.internal;

import openclaw.plugin.sdk.annotation.InternalApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Helper class for loading services using ServiceLoader.
 * <p>
 * This is an internal class and should not be used directly by plugins.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@InternalApi(reason = "Internal service loading implementation")
public final class ServiceLoaderHelper {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoaderHelper.class);

    private ServiceLoaderHelper() {
        // Utility class
    }

    /**
     * Load all services of the given type.
     *
     * @param <T> the service type
     * @param serviceType the service interface
     * @return list of loaded services
     */
    public static <T> List<T> loadServices(Class<T> serviceType) {
        List<T> services = new ArrayList<>();
        ServiceLoader<T> loader = ServiceLoader.load(serviceType);

        for (T service : loader) {
            services.add(service);
            logger.debug("Loaded service: {} -> {}", serviceType.getName(), service.getClass().getName());
        }

        logger.info("Loaded {} services of type {}", services.size(), serviceType.getName());
        return services;
    }

    /**
     * Load a single service of the given type.
     *
     * @param <T> the service type
     * @param serviceType the service interface
     * @return optional of the first found service
     */
    public static <T> Optional<T> loadFirstService(Class<T> serviceType) {
        ServiceLoader<T> loader = ServiceLoader.load(serviceType);
        return loader.findFirst();
    }

    /**
     * Load services with a specific classloader.
     *
     * @param <T> the service type
     * @param serviceType the service interface
     * @param classLoader the classloader to use
     * @return list of loaded services
     */
    public static <T> List<T> loadServices(Class<T> serviceType, ClassLoader classLoader) {
        List<T> services = new ArrayList<>();
        ServiceLoader<T> loader = ServiceLoader.load(serviceType, classLoader);

        for (T service : loader) {
            services.add(service);
        }

        return services;
    }
}
