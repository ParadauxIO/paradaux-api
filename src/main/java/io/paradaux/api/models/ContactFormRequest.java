package io.paradaux.api.models;

import lombok.Data;

@Data
public class ContactFormRequest {
    private String name;
    private String email;
    private String subject;
    private String message;
    private String cfTurnstileResponse;
}
