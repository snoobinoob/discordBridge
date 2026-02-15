package discordbridge;

import necesse.engine.modLoader.ModSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class Settings extends ModSettings {
    public static String token = "";
    public static String channelID = "";

    public static boolean areInvalid() {
        return token.isEmpty() || channelID.isEmpty();
    }

    @Override
    public void applyLoadData(LoadData loadData) {
        token = loadData.getSafeString("token", "");
        channelID = loadData.getSafeString("channelID", "");
    }

    @Override
    public void addSaveData(SaveData saveData) {
        saveData.addSafeString("token", token);
        saveData.addSafeString("channelID", channelID);
    }
}
