package discordbridge.websocket.message;

import discordbridge.websocket.OpCodes;
import mjson.Json;
import necesse.engine.network.server.Server;

public class PresenceMessage extends WebSocketMessage {
    private static final int WATCHING_ACTIVITY = 3;

    public PresenceMessage(Server server) {
        super(buildContent(server));
    }

    private static Json buildContent(Server server) {
        int numOnline = server.getPlayersOnline();
        int numSlots = server.getSlots();
        String statusMessage = String.format("Online Players (%d/%d)", numOnline, numSlots);
        return Json.object()
                .set("op", OpCodes.PRESENCE_UPDATE)
                .set("d", Json.object()
                        .set("since", null)
                        .set("status", "online")
                        .set("afk", false)
                        .set("activities", Json.array().add(Json.object()
                                .set("type", WATCHING_ACTIVITY)
                                .set("name", statusMessage)
                                .set("details", "Custom activity")
                        ))
                );
    }
}
