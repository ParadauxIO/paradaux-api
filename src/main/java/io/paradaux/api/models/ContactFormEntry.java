package io.paradaux.api.models;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ContactFormEntry {
    private int id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private String ipAddress;
    private Timestamp createdAt;

    public static ContactFormEntry of(ContactFormRequest request) {
        ContactFormEntry entry = new ContactFormEntry();
        entry.setName(request.getName());
        entry.setEmail(request.getEmail());
        entry.setSubject(request.getSubject());
        entry.setMessage(request.getMessage());
        entry.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return entry;
    }
}
