package discordbridge;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.events.ServerStopEvent;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.annotations.ModEntry;

@ModEntry
public class DiscordBridgeEntry {
    private static DiscordBot bot;

    public void init() {
        System.out.println("Hello world from discord bridge!");
    }

    public ModSettings initSettings() {
        return new Settings();
    }

    public void postInit() {
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent serverStartEvent) {
                bot = new DiscordBot();
                Thread botThread = new Thread(bot);
                botThread.start();
            }
        });

        GameEvents.addListener(ServerStopEvent.class, new GameEventListener<ServerStopEvent>() {
            @Override
            public void onEvent(ServerStopEvent serverStopEvent) {
                bot.stop();
            }
        });
    }
}
