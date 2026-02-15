package discordbridge.websocket.message;

import discordbridge.Settings;
import discordbridge.websocket.OpCodes;
import mjson.Json;

public class IdentifyMessage extends WebSocketMessage {
    public static final int GUILD_MESSAGES = 1 << 9;
    public static final int MESSAGE_CONTENT = 1 << 15;

    public IdentifyMessage() {
        super(Json.object()
                .set("op", OpCodes.IDENTIFY)
                .set("d", Json.object()
                        .set("token", Settings.token)
                        .set("properties", Json.object()
                                .set("os", System.getProperty("os.name"))
                                .set("browser", "Necesse")
                                .set("device", "Necesse")
                        )
                        .set("intents", GUILD_MESSAGES | MESSAGE_CONTENT)
                )
        );
    }
}
