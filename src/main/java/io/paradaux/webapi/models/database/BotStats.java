package io.paradaux.webapi.models.database;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.io.Serializable;

public class BotStats implements Serializable {

    public static final long serialVersionUID = 1L;

    @BsonProperty("guild_count")
    private int guildCount;

    @BsonProperty("user_count")
    private int userCount;

    @BsonProperty("lines_of_code")
    private int codeLinesCount;

    public BotStats() {

    }

    public BotStats(int guildCount, int userCount, int codeLinesCount) {
        this.guildCount = guildCount;
        this.userCount = userCount;
        this.codeLinesCount = codeLinesCount;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public int getGuildCount() {
        return guildCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public int getCodeLinesCount() {
        return codeLinesCount;
    }

    public void setGuildCount(int guildCount) {
        this.guildCount = guildCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public void setCodeLinesCount(int codeLinesCount) {
        this.codeLinesCount = codeLinesCount;
    }
}
