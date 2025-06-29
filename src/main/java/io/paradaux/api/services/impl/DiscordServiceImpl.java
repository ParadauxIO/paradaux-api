package io.paradaux.api.services.impl;

import io.paradaux.api.models.ContactFormRequest;
import io.paradaux.api.services.DiscordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class DiscordServiceImpl implements DiscordService {

    private final WebClient webClient;

    @Value("${discord.webhook}")
    private String webhookUrl;

    public DiscordServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://discord.com/api/webhooks").build();
    }

    public Mono<Void> sendContactForm(ContactFormRequest request) {
        String content = String.format(
                """
                **New Contact Form Submission:**
                **Name:** %s
                **Email:** %s
                **Subject:** %s
                **Message:** %s
                """,
                request.getName(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage()
        );

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);

        String webhookPath = webhookUrl.replace("https://discord.com/api/webhooks", "");

        return webClient.post()
                .uri(webhookPath)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }
}