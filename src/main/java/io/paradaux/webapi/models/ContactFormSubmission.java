package io.paradaux.webapi.models;

import java.io.Serializable;

public class ContactFormSubmission implements Serializable {

    public static final long serialVersionUID = 1L;

    private final String email;
    private final String name;
    private final String subject;
    private final String message;

    public ContactFormSubmission(String email, String name, String subject, String message) {
        this.email = email;
        this.name = name;
        this.subject = subject;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }
}
