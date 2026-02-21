package discordbridge.websocket.message;

import discordbridge.Settings;
import discordbridge.websocket.OpCodes;
import discordbridge.websocket.ReconnectData;
import mjson.Json;

public class ResumeMessage extends WebSocketMessage {
    public ResumeMessage(ReconnectData data, Object sequenceNumber) {
        super(Json.object()
                .set("op", OpCodes.RESUME)
                .set("d", Json.object()
                        .set("token", Settings.token)
                        .set("session_id", data.sessionID)
                        .set("seq", sequenceNumber)
                )
        );
    }
}
