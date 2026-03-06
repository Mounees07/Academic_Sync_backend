package com.academic.platform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Fine-grained cache configuration.
 *
 * Scalability improvement: Instead of hitting the database on every API request,
 * frequently read entities (users, system settings) are cached in-memory
 * using Caffeine — a high-performance caching library.
 *
 * Cache names used:
 *   - "users"         : User entity by firebaseUid — TTL 10 min
 *   - "settings"      : System settings — TTL 5 min (refresh less often)
 *   - "enrollments"   : Student course enrollments — TTL 3 min
 *   - "faculty"       : Faculty lists by department — TTL 10 min
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
            buildCache("users",       500, 10, TimeUnit.MINUTES),
            buildCache("settings",    100,  5, TimeUnit.MINUTES),
            buildCache("enrollments", 1000, 3, TimeUnit.MINUTES),
            buildCache("faculty",     200, 10, TimeUnit.MINUTES),
            buildCache("courses",     500,  5, TimeUnit.MINUTES)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int maxSize, long duration, TimeUnit unit) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(duration, unit)
                .recordStats()  // enables cache statistics for monitoring
                .build());
    }
}
