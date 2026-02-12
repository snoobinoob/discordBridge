package discordbridge;

import necesse.engine.modLoader.ModSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class Settings extends ModSettings {
    public static String token = "";

    @Override
    public void applyLoadData(LoadData loadData) {
        token = loadData.getSafeString("token", "");
    }

    @Override
    public void addSaveData(SaveData saveData) {
        saveData.addSafeString("token", token);
    }
}
