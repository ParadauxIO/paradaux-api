package io.paradaux.api.services;

import io.paradaux.api.models.ContactFormRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface ContactService {
    Mono<ResponseEntity<String>> processContactForm(ContactFormRequest request);
}
