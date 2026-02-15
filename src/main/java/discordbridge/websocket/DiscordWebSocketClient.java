package discordbridge.websocket;

import discordbridge.DiscordBot;
import discordbridge.Utils;
import discordbridge.websocket.message.HeartbeatMessage;
import discordbridge.websocket.message.IdentifyMessage;
import discordbridge.websocket.message.WebSocketMessage;
import mjson.Json;
import necesse.engine.GameLog;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class DiscordWebSocketClient extends WebSocketClient {
    private enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    private State state;
    private Object heartbeatSequenceNumber;
    private Timer heartbeatTimer;

    public DiscordWebSocketClient(URI websocketUri) {
        super(websocketUri);
        state = State.DISCONNECTED;
    }

    @Override
    public void connect() {
        super.connect();
        state = State.CONNECTING;
        heartbeatSequenceNumber = null;
        heartbeatTimer = new Timer("Discord-Heartbeat");
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        Utils.debug("[WS] connection opened");
    }

    @Override
    public void onMessage(String message) {
        Json json = Json.read(message);
        Utils.debug("[WS] Received message: " + message);
        if (json.has("s")) {
            heartbeatSequenceNumber = json.at("s").getValue();
            Utils.debug("[WS] Set sequence to: " + heartbeatSequenceNumber);
        }
        if (isMessageType(json, OpCodes.HEARTBEAT)) {
            send(new HeartbeatMessage(heartbeatSequenceNumber));
        } else if (state == State.CONNECTING) {
            processConnectingMessage(json);
        } else if (state == State.CONNECTED) {
            processedConnectedMessage(json);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Utils.log(String.format("[WS] Connection closed (%d: %s)", code, reason));
        heartbeatTimer.cancel();
    }

    @Override
    public void onError(Exception e) {
        Utils.warn("[WS] Error: " + e.getMessage());
        e.printStackTrace(GameLog.debug);
    }

    public void send(WebSocketMessage message) {
        if (isOpen()) {
            send(message.toString());
        }
    }

    private void processConnectingMessage(Json messageJson) {
        if (isMessageType(messageJson, OpCodes.HELLO)) {
            Json intervalJson = Utils.atPath(messageJson, "d.heartbeat_interval");
            if (intervalJson == null) {
                Utils.warn("[WS] Missing heartbeat interval in HELLO event");
                return;
            }
            heartbeatTimer.scheduleAtFixedRate(new SendHeartbeatTask(), 0, intervalJson.asInteger());
            Utils.debug("[WS] Sending identify message");
            send(new IdentifyMessage());
            DiscordBot.updatePresence(0);
        } else if (isMessageType(messageJson, OpCodes.HEARTBEAT_ACK)) {
            Utils.debug("[WS] Received heartbeat acknowledgment");
        } else if (isMessageType(messageJson, OpCodes.READY)) {
            Utils.log("[WS] Authentication complete!");
            state = State.CONNECTED;
        }
    }

    private void processedConnectedMessage(Json messageJson) {
        if (isMessageType(messageJson, OpCodes.MESSAGE_CREATE)) {
            DiscordBot.handleChatMessage(messageJson);
        }
    }

    private static boolean isMessageType(Json messageJson, int opcode) {
        return Utils.hasMatchingAttribute(messageJson, "op", opcode);
    }

    private static boolean isMessageType(Json messageJson, String type) {
        boolean isOpZero = Utils.hasMatchingAttribute(messageJson, "op", 0);
        boolean isType = Utils.hasMatchingAttribute(messageJson, "t", type);
        return isOpZero && isType;
    }

    private class SendHeartbeatTask extends TimerTask {
        @Override
        public void run() {
            Utils.debug("[WS] Sending heartbeat (" + heartbeatSequenceNumber + ")");
            send(new HeartbeatMessage(heartbeatSequenceNumber));
        }
    }
}
