package io.paradaux.api.controllers;

import io.paradaux.api.models.ContactFormRequest;
import io.paradaux.api.services.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public Mono<ResponseEntity<String>> handleContactForm(@RequestBody ContactFormRequest request) {
        return contactService.processContactForm(request);
    }
}