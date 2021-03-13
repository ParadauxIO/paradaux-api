package io.paradaux.webapi.models;

import com.google.gson.annotations.SerializedName;

public class Configuration {

    @SerializedName("mongo_uri")
    private final String mongoUri;

    public Configuration(String mongoUri) {
        this.mongoUri = mongoUri;
    }

    public String getMongoUri() {
        return mongoUri;
    }
}
