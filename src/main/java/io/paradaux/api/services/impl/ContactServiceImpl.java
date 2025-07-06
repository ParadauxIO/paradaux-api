package io.paradaux.api.services.impl;

import io.paradaux.api.mappers.ContactFormMapper;
import io.paradaux.api.models.ContactFormEntry;
import io.paradaux.api.models.ContactFormRequest;
import io.paradaux.api.services.CloudflareTurnstileService;
import io.paradaux.api.services.ContactService;
import io.paradaux.api.services.DiscordService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ContactServiceImpl implements ContactService {

    private final CloudflareTurnstileService turnstileService;
    private final DiscordService discordService;
    private final ContactFormMapper contactFormMapper;

    public ContactServiceImpl(CloudflareTurnstileService turnstileService, DiscordService discordService, ContactFormMapper contactFormMapper) {
        this.turnstileService = turnstileService;
        this.discordService = discordService;
        this.contactFormMapper = contactFormMapper;
    }

    @Override
    public Mono<ResponseEntity<String>> processContactForm(ContactFormRequest request) {
        ContactFormEntry entry = ContactFormEntry.of(request);

        return turnstileService.verifyToken(request.getCfTurnstileResponse())
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.just(ResponseEntity.badRequest().body("Turnstile validation failed."));
                    }

                    return Mono.fromRunnable(() -> contactFormMapper.insert(entry))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then(discordService.sendContactForm(request))
                            .then(Mono.just(ResponseEntity.ok("Message received.")));
                });
    }
}
