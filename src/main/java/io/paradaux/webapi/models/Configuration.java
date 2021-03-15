package io.paradaux.webapi.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Configuration implements Serializable {

    public static final long serialVersionUID = 2L;

    @SerializedName("mongo_uri")
    private final String mongoUri;

    @SerializedName("contact_form_webhook")
    private final String contactFormWebhook;

    public Configuration(String mongoUri, String contactFormWebhook) {
        this.mongoUri = mongoUri;
        this.contactFormWebhook = contactFormWebhook;
    }

    public String getMongoUri() {
        return mongoUri;
    }

    public String getContactFormWebhook() {
        return contactFormWebhook;
    }
}
