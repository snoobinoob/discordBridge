package discordbridge;

import mjson.Json;

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
}
