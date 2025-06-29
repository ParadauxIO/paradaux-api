package io.paradaux.api.services;

import io.paradaux.api.models.ContactFormRequest;
import reactor.core.publisher.Mono;

public interface DiscordService {
    Mono<Void> sendContactForm(ContactFormRequest request);
}
