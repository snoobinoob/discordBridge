package discordbridge;

import com.vdurmont.emoji.EmojiParser;
import mjson.Json;
import necesse.engine.GameLog;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String getStringOrNull(Json data, String path) {
        return getStringOrElse(data, path, null);
    }

    public static String getStringOrElse(Json data, String path, String defaultValue) {
        Json value = atPath(data, path);
        return value != null && value.isString() ? value.asString() : defaultValue;
    }

    public static boolean hasMatchingAttribute(Json data, String path, int expectedValue) {
        Json value = atPath(data, path);
        return value != null && value.isNumber() && value.asInteger() == expectedValue;
    }

    public static boolean hasMatchingAttribute(Json data, String path, String expectedValue) {
        Json value = atPath(data, path);
        return value != null && value.isString() && value.asString().equals(expectedValue);
    }

    public static Json atPath(Json data, String path) {
        Json value = data;
        for (String attr : path.split("\\.")) {
            if (!value.has(attr)) {
                return null;
            }
            value = value.at(attr);
        }
        return value;
    }

    public static void log(String message) {
        log(GameLog.out, message);
    }

    public static void debug(String message) {
        log(GameLog.debug, message);
    }

    public static void warn(String message) {
        log(GameLog.warn, message);
    }

    public static void error(String message) {
        log(GameLog.err, message);
    }

    private static void log(PrintStream stream, String message) {
        stream.println("[DiscordBridge] " + message);
    }

    public static String getDiscordMessage(String author, String message) {
        return EmojiParser.parseToUnicode(Settings.discordMessageFormat.replace("<author>", author).replace("<message>", message));
    }

    public static String getDiscordConnectMessage(String playerName) {
        return EmojiParser.parseToUnicode(Settings.discordConnectMessageFormat.replace("<player>", playerName));
    }

    public static String getDiscordDisconnectMessage(String playerName) {
        return EmojiParser.parseToUnicode(Settings.discordDisconnectMessageFormat.replace("<player>", playerName));
    }

    public static String getNecesseMessage(Json messageJson) {
        Json data = messageJson.at("d");
        String author = getAuthor(data);
        String message = getStringOrNull(data, "content");
        message = EmojiParser.parseToAliases(replaceMentions(message, data), EmojiParser.FitzpatrickAction.REMOVE);

        return Settings.necesseMessageFormat.replace("<author>", author).replace("<message>", message);
    }

    private static String getAuthor(Json data) {
        String author = getStringOrNull(data, "member.nick");
        if (author == null) {
            author = getStringOrNull(data, "author.global_name");
        }
        if (author == null) {
            author = getStringOrNull(data, "author.username");
        }
        return author;
    }

    private static String replaceMentions(String message, Json data) {
        String processedMessage = message;

        // User mentions
        List<Json> userMentions = data.at("mentions").asJsonList();
        for (Json userMention : userMentions) {
            String userID = userMention.at("id").asString();
            String userName = getUserName(userMention);
            processedMessage = processedMessage.replaceAll("<@" + userID + ">", "@" + userName);
        }

        // Role mentions
        List<Json> roleIdList = data.at("mention_roles").asJsonList();
        if (!roleIdList.isEmpty()) {
            String guildID = data.at("guild_id").asString();
            Json response = DiscordBot.makeRequest("get", "/guilds/" + guildID + "/roles");
            List<Json> discordRoles = response.asJsonList();
            for (Json roleIdJson : roleIdList) {
                String roleID = roleIdJson.asString();
                Optional<Json> matchingRole = discordRoles.stream()
                        .filter(roleJson -> roleJson.at("id").asString().equals(roleID))
                        .findAny();
                String roleName = "unknown-role";
                if (matchingRole.isPresent()) {
                    roleName = matchingRole.get().at("name").asString();
                }
                processedMessage = processedMessage.replaceAll("<@&" + roleID + ">", "@" + roleName);
            }
        }

        // Channel mentions
        Matcher channelMentionMatcher = Pattern.compile("<#(\\d+)>").matcher(processedMessage);
        Set<String> channelIDs = new HashSet<>();
        while (channelMentionMatcher.find()) {
            channelIDs.add(channelMentionMatcher.group(1));
        }
        if (!channelIDs.isEmpty()) {
            String guildID = data.at("guild_id").asString();
            List<Json> channels = DiscordBot.makeRequest("get", "/guilds/" + guildID + "/channels").asJsonList();
            for (String channelID : channelIDs) {
                Optional<Json> matchingChannel = channels.stream()
                        .filter(channelJson -> channelJson.at("id").asString().equals(channelID))
                        .findAny();
                String channelName = "unknown";
                if (matchingChannel.isPresent()) {
                    channelName = matchingChannel.get().at("name").asString();
                }
                processedMessage = processedMessage.replaceAll("<#" + channelID + ">", "#" + channelName);
            }
        }
        return processedMessage;
    }

    private static String getUserName(Json data) {
        String name = getStringOrNull(data, "member.nick");
        if (name == null) {
            name = getStringOrNull(data, "global_name");
        }
        if (name == null) {
            name = getStringOrElse(data, "username", "<unknown>");
        }
        return name;
    }
}
