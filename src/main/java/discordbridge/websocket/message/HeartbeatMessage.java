package discordbridge.websocket.message;

import discordbridge.websocket.OpCodes;
import mjson.Json;

public class HeartbeatMessage extends WebSocketMessage {
    public HeartbeatMessage(Object sequenceNumber) {
        super(Json.object()
                .set("op", OpCodes.HEARTBEAT)
                .set("d", sequenceNumber)
        );
    }
}
