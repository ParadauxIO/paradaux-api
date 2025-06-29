package io.paradaux.api.services.impl;

import io.paradaux.api.models.ContactFormRequest;
import io.paradaux.api.services.CloudflareTurnstileService;
import io.paradaux.api.services.ContactService;
import io.paradaux.api.services.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ContactServiceImpl implements ContactService {

    private final CloudflareTurnstileService turnstileService;
    private final DiscordService discordService;

    @Autowired
    public ContactServiceImpl(CloudflareTurnstileService turnstileService, DiscordService discordService) {
        this.turnstileService = turnstileService;
        this.discordService = discordService;
    }

    @Override
    public Mono<ResponseEntity<String>> processContactForm(ContactFormRequest request) {
        return turnstileService.verifyToken(request.getCfTurnstileResponse())
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.just(ResponseEntity.badRequest().body("Turnstile validation failed."));
                    }

                    return discordService.sendContactForm(request)
                            .then(Mono.just(ResponseEntity.ok("Message received.")));
                });
    }
}
