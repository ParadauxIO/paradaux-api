package io.paradaux.webapi.managers;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import io.paradaux.webapi.models.Configuration;
import io.paradaux.webapi.models.ContactFormSubmission;
import org.springframework.lang.NonNull;

public class DiscordWebhookManager {

    private static final String SERVICE_NAME = "Contact Form Submitted";
    private static final String ICON_URL = "https://cdn.paradaux.io/img/wc315.png";
    private static final String PROVIDER = "paradaux.io";
    private static final String URL = "https://paradaux.io";

    private final WebhookClient client;
    private final WebhookEmbed.EmbedAuthor author = new WebhookEmbed.EmbedAuthor(SERVICE_NAME, ICON_URL, URL);

    public DiscordWebhookManager(Configuration config) {
        WebhookClientBuilder builder = new WebhookClientBuilder(config.getContactFormWebhook());
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName(PROVIDER);
            thread.setDaemon(true);
            return thread;
        });

        builder.setWait(true);
        client = builder.build();
    }

    public void postContactForm(@NonNull ContactFormSubmission form) {
        String name = (form.getName() != null) ? form.getName() : "N/A";
        String email = (form.getEmail() != null) ? form.getEmail() : "N/A";
        String subject = (form.getSubject() != null) ? form.getSubject() : "N/A";
        String message = (form.getMessage() != null) ? form.getMessage() : "N/A";

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setAvatarUrl(ICON_URL)
                .setUsername(String.format("%s » %s", PROVIDER, SERVICE_NAME))
                .addEmbeds(
                        new WebhookEmbedBuilder()
                                .setColor(0x008080)
                                .setAuthor(author)
                                .addField(new WebhookEmbed.EmbedField(true, "Name", name))
                                .addField(new WebhookEmbed.EmbedField(true, "Email", email))
                                .addField(new WebhookEmbed.EmbedField(true, "Subject", subject))
                                .addField(new WebhookEmbed.EmbedField(true, "Message", message))
                                .build()
                );

        client.send(messageBuilder.build());
    }


}
