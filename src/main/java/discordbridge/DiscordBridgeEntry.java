package discordbridge;

import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.annotations.ModEntry;

@ModEntry
public class DiscordBridgeEntry {

    public void init() {
        System.out.println("Hello world from discord bridge!");
    }

    public ModSettings initSettings() {
        return new Settings();
    }

    public void postInit() {
        Thread botThread = new Thread(new DiscordBot());
        botThread.start();
    }
}
