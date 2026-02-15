package discordbridge;

import mjson.Json;
import necesse.engine.GameLog;

import java.io.PrintStream;

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
}
