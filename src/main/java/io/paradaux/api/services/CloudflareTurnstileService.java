package io.paradaux.api.services;

import reactor.core.publisher.Mono;

public interface CloudflareTurnstileService {

    Mono<Boolean> verifyToken(String token);
}
