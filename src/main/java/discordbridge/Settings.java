package discordbridge;

import necesse.engine.GameLog;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings extends ModSettings {
    public static String token = "";
    public static String channelID = "";
    public static String discordMessageFormat = "[Necesse] **<author>**: <message>";
    public static String discordConnectMessageFormat = "[Necesse] _<player> has joined_";
    public static String discordDisconnectMessageFormat = "[Necesse] _<player> has left_";
    public static String necesseMessageFormat = "[§8Discord§0] <author>: <message>";

    private static final Path overridePath = Paths.get("discord_bridge.txt");
    private static final Pattern settingAssignmentPattern = Pattern.compile("^(\\w+)\\s*=\\s*(.+?)$");
    private static Set<String> overriddenFields;

    public Settings() {
        overriddenFields = new HashSet<>();
        if (!Files.exists(overridePath)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(overridePath);
        } catch (IOException e) {
            Utils.warn("Error reading settings override file: " + e.getMessage());
            return;
        }
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            Matcher assignmentMatcher = settingAssignmentPattern.matcher(line);
            if (assignmentMatcher.find()) {
                String name = assignmentMatcher.group(1);
                String value = assignmentMatcher.group(2);
                for (Field field : Settings.class.getDeclaredFields()) {
                    if (field.getName().equalsIgnoreCase(name)) {
                        try {
                            field.set(null, value);
                            overriddenFields.add(field.getName());
                        } catch (IllegalAccessException e) {
                            Utils.warn("Error setting " + name + " from override file");
                        }
                        break;
                    }
                }
            }
        }
    }

    public static boolean areInvalid() {
        return token.isEmpty() || channelID.isEmpty();
    }

    @Override
    public void applyLoadData(LoadData save) {
        for (Field field : Settings.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (overriddenFields.contains(fieldName)
                    || !save.hasLoadDataByName(fieldName)
                    || !Modifier.isPublic(field.getModifiers())
            ) {
                continue;
            }

            try {
                field.set(null, save.getSafeString(fieldName));
            } catch (IllegalAccessException e) {
                Utils.warn("Error setting " + fieldName + " from config file");
            }
        }
    }

    @Override
    public void addSaveData(SaveData save) {
        for (Field field : Settings.class.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            try {
                save.addSafeString(field.getName(), (String) field.get(null));
            } catch (IllegalAccessException e) {
                GameLog.warn.println("Error saving " + field.getName() + " to config file");
            }
        }
    }
}
