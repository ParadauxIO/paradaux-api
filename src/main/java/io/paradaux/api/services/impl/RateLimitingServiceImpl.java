package io.paradaux.api.services.impl;

import io.paradaux.api.services.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingServiceImpl implements RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long TIME_WINDOW = 60_000; // 1 min
    private static final int MAX_REQUESTS = 75;

    @Override
    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        String redisKey = "rate:" + key;
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, now - TIME_WINDOW);
        redisTemplate.opsForZSet().add(redisKey, UUID.randomUUID().toString(), now);
        redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        Long count = redisTemplate.opsForZSet().zCard(redisKey);
        log.info("Rate limiter check for key: {}, count: {}", key, count);
        return count != null && count <= MAX_REQUESTS;
    }
}
