package io.paradaux.api.services;

public interface RateLimitingService {
    boolean isAllowed(String key);
}
