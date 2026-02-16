package discordbridge;

import necesse.engine.modLoader.ModSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class Settings extends ModSettings {
    public static String token = "";
    public static String channelID = "";
    public static String discordMessageFormat = "[Necesse] **<author>**: <message>";
    public static String necesseMessageFormat = "[Discord] <author>: <message>";

    public static boolean areInvalid() {
        return token.isEmpty() || channelID.isEmpty();
    }

    @Override
    public void applyLoadData(LoadData loadData) {
        token = loadData.getSafeString("token", token);
        channelID = loadData.getSafeString("channelID", channelID);
        discordMessageFormat = loadData.getSafeString("discordMessageFormat", discordMessageFormat);
        necesseMessageFormat = loadData.getSafeString("necesseMessageFormat", necesseMessageFormat);
    }

    @Override
    public void addSaveData(SaveData saveData) {
        saveData.addSafeString("token", token);
        saveData.addSafeString("channelID", channelID);
        saveData.addSafeString("discordMessageFormat", discordMessageFormat);
        saveData.addSafeString("necesseMessageFormat", necesseMessageFormat);
    }
}
