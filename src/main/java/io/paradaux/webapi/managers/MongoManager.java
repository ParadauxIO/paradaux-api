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
import io.paradaux.webapi.models.database.BotStats;
import io.paradaux.webapi.models.database.ContactFormSubmission;
import io.paradaux.webapi.models.database.TagEntry;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoManager {

    private static final String BOT_DATABASE_NAME = "friendlybot";
    private static final String DATABASE_NAME = "paradaux_webapi";

    private final Logger logger = LoggingManager.getLogger();

    // WebAPI collections
    private MongoCollection<ContactFormSubmission> contactForms;

    // FriendlyBot Collections
    private MongoCollection<TagEntry> botTags;
    private MongoCollection<BotStats> botStats;

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

        MongoDatabase database = client.getDatabase(DATABASE_NAME);
        MongoDatabase botDatabase = client.getDatabase(BOT_DATABASE_NAME);

        contactForms = database.getCollection("contact_forms", ContactFormSubmission.class);
        botTags = botDatabase.getCollection("tags", TagEntry.class);
        botStats = botDatabase.getCollection("stats", BotStats.class);

        if(botStats.countDocuments() == 0) {
            botStats.insertOne(new BotStats(1, 1, 1));
        }
    }

    public FindIterable<TagEntry> getTagsFromServer(String serverId) {
        return botTags.find(Filters.eq("guild_id", serverId));
    }

    public void insertContactForm(ContactFormSubmission form) {
        contactForms.insertOne(form);
    }

    public BotStats getBotStats() {
        return botStats.find().first();
    }
}
