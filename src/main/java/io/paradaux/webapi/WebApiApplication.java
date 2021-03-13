package io.paradaux.webapi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.paradaux.webapi.managers.IOManager;
import io.paradaux.webapi.managers.MongoManager;
import io.paradaux.webapi.models.Configuration;
import io.paradaux.webapi.models.database.TagEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;

@RestController
@SpringBootApplication(exclude ={MongoAutoConfiguration.class})
public class WebApiApplication {

    private final MongoManager mongo;

    public WebApiApplication() throws FileNotFoundException {
        IOManager.deployFiles();

        Configuration configuration = new IOManager().readConfigurationFile();

        mongo = new MongoManager(configuration);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebApiApplication.class, args);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/friendly-bot/tags/{serverid}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public String handleInterestForms(@PathVariable("serverid") String serverId) {
        Gson gson = new Gson();
        JsonArray array = new JsonArray();

        for (final TagEntry tag : mongo.getTagsFromServer(serverId)) {
            String jsonString = gson.toJson(tag, TagEntry.class);
            array.add(gson.fromJson(jsonString, JsonObject.class));
        }

        return gson.toJson(array);
    }
}
