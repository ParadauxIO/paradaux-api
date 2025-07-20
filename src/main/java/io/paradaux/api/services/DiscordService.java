package io.paradaux.api.services;

import io.paradaux.api.models.ContactFormRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DiscordService {
    Mono<Void> sendContactForm(ContactFormRequest request);
    void sendMessage(String title, String description, Map<String, String> fields);
}
