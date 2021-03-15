package io.paradaux.webapi.models;

import com.google.gson.annotations.SerializedName;

public class Configuration {

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
}
