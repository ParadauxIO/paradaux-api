package io.paradaux.webapi.models.database;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.io.Serializable;
import java.util.Date;

public class TagEntry implements Serializable {

    protected static final long serialVersionUID = 1L;

    @BsonProperty(value = "guild_id")
    private String guild;

    @BsonProperty(value = "discord_id")
    private String discordId;

    @BsonProperty(value = "id")
    private String id;

    @BsonProperty(value = "content")
    private String content;

    @BsonProperty(value = "time_created")
    private Date timeCreated;

    public TagEntry() {

    }

    public TagEntry(String guild, String discordId, String id, String content, Date timeCreated) {
        this.guild = guild;
        this.discordId = discordId;
        this.id = id;
        this.content = content;
        this.timeCreated = timeCreated;
    }

    public String getGuild() {
        return guild;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public TagEntry setGuild(String guild) {
        this.guild = guild;
        return this;
    }

    public TagEntry setDiscordId(String discordId) {
        this.discordId = discordId;
        return this;
    }

    public TagEntry setId(String id) {
        this.id = id;
        return this;
    }

    public TagEntry setContent(String content) {
        this.content = content;
        return this;
    }

    public TagEntry setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

}