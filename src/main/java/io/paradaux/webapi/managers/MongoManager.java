package io.paradaux.webapi.managers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.paradaux.webapi.models.Configuration;
import io.paradaux.webapi.models.database.TagEntry;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoManager {

    private static final String DATABASE_NAME = "friendlybot";
    private final Logger logger = LoggingManager.getLogger();

    private MongoCollection<TagEntry> tags;

    public MongoManager(Configuration configuration) {
        logger.info("Initialising: Database Controller");
        ConnectionString connectionString = new ConnectionString(configuration.getMongoUri());

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider
                .builder()
                .automatic(true)
                .build());

        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings
                .getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();

        MongoClient client;

        try {
            client = MongoClients.create(clientSettings);
        } catch (Exception exception) {
            logger.error("There was an error logging in to MongoDB", exception);
            return;
        }

        MongoDatabase dataBase = client.getDatabase(DATABASE_NAME);
        tags = dataBase.getCollection("tags", TagEntry.class);
    }

    public FindIterable<TagEntry> getTagsFromServer(String serverId) {
        return tags.find(Filters.eq("guild_id", serverId));
    }
}
