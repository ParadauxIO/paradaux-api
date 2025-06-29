package io.paradaux.api.services.impl;

import io.paradaux.api.services.CloudflareTurnstileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class CloudflareTurnstileServiceImpl implements CloudflareTurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private final WebClient webClient = WebClient.create();

    @Value("${cloudflare.turnstile.secret}")
    private String secretKey;

    public Mono<Boolean> verifyToken(String token) {
        return webClient.post()
                .uri(VERIFY_URL)
                .bodyValue(Map.of(
                        "secret", secretKey,
                        "response", token
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> Boolean.TRUE.equals(response.get("success")))
                .onErrorReturn(false);
    }

}
