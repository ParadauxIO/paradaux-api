package io.paradaux.api.services.impl;

import io.paradaux.api.models.ContactFormRequest;
import io.paradaux.api.services.DiscordService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public void sendMessage(String title, String description, Map<String, String> fields) {
        Map<String, Object> embed = getEmbed(title, description, fields);
        Map<String, Object> body = new HashMap<>();
        body.put("embeds", List.of(embed));

        String webhookPath = webhookUrl.replace("https://discord.com/api/webhooks", "");

        webClient.post()
                .uri(webhookPath)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(); // blocking call
    }

    @NotNull
    private static Map<String, Object> getEmbed(String title, String description, Map<String, String> fields) {
        List<Map<String, Object>> embedFields = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            Map<String, Object> field = new HashMap<>();
            field.put("name", entry.getKey());
            field.put("value", entry.getValue());
            field.put("inline", false);
            embedFields.add(field);
        }

        Map<String, Object> embed = new HashMap<>();
        embed.put("title", title);
        embed.put("description", description);
        embed.put("color", 0x3498DB);
        embed.put("fields", embedFields);
        return embed;
    }
}