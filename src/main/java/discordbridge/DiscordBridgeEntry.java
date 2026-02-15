package discordbridge;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.events.ServerClientDisconnectEvent;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.events.ServerStopEvent;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.annotations.ModEntry;

@ModEntry
public class DiscordBridgeEntry {
    public void init() {
    }

    public ModSettings initSettings() {
        return new Settings();
    }

    public void postInit() {
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent serverStartEvent) {
                if (Settings.areInvalid()) {
                    Utils.warn("Not configured!");
                    return;
                }
                DiscordBot.init(serverStartEvent.server);
                Thread botThread = new Thread(DiscordBot.instance);
                botThread.start();
            }
        });

        GameEvents.addListener(ServerStopEvent.class, new GameEventListener<ServerStopEvent>() {
            @Override
            public void onEvent(ServerStopEvent serverStopEvent) {
                DiscordBot.stop();
            }
        });

        GameEvents.addListener(ServerClientConnectedEvent.class, new GameEventListener<ServerClientConnectedEvent>() {
            @Override
            public void onEvent(ServerClientConnectedEvent serverClientConnectedEvent) {
                DiscordBot.updatePresence();
            }
        });

        GameEvents.addListener(ServerClientDisconnectEvent.class, new GameEventListener<ServerClientDisconnectEvent>() {
            @Override
            public void onEvent(ServerClientDisconnectEvent serverClientDisconnectEvent) {
                DiscordBot.updatePresence();
            }
        });
    }
}
