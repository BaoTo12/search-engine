package com.chibao.edu.search_engine.infrastructure.cache.redis.adapter;

import com.chibao.edu.search_engine.application.search.dto.SearchResponseDTO;
import com.chibao.edu.search_engine.application.search.port.output.SearchCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Infrastructure Adapter: Implements SearchCachePort using Redis.
 * This adapter handles Redis-specific caching logic.
 */
@Component
public class SearchCacheRedisAdapter implements SearchCachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "search:";

    public SearchCacheRedisAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SearchResponseDTO get(String cacheKey) {
        try {
            String fullKey = CACHE_KEY_PREFIX + cacheKey;
            return (SearchResponseDTO) redisTemplate.opsForValue().get(fullKey);
        } catch (Exception e) {
            // Log error but don't fail - cache miss is acceptable
            return null;
        }
    }

    @Override
    public void put(String cacheKey, SearchResponseDTO response, int ttlMinutes) {
        try {
            String fullKey = CACHE_KEY_PREFIX + cacheKey;
            redisTemplate.opsForValue().set(fullKey, response, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Log error but don't fail - application should continue without cache
        }
    }

    @Override
    public void clear() {
        try {
            var keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Log error
        }
    }
}
