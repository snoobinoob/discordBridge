package discordbridge.websocket.message;

import mjson.Json;

public abstract class WebSocketMessage {
    private final Json messageData;

    public WebSocketMessage(Json messageData) {
        this.messageData = messageData;
    }

    @Override
    public String toString() {
        return messageData.toString();
    }
}
