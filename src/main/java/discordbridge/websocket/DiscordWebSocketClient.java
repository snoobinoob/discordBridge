package discordbridge.websocket;

import discordbridge.websocket.message.HeartbeatMessage;
import discordbridge.websocket.message.IdentifyMessage;
import discordbridge.websocket.message.WebSocketMessage;
import mjson.Json;
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

    public DiscordWebSocketClient(URI serverUri) {
        super(serverUri);
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
        System.out.println("[WS] Connected!");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Received message: " + message);
        Json json = Json.read(message);
        if (json.has("s")) {
            System.out.println("[WS] Setting seq to: " + json.at("s").getValue());
            heartbeatSequenceNumber = json.at("s").getValue();
        }
        if (state == State.CONNECTING) {
            processConnectingMessage(json);
        } else if (state == State.CONNECTED) {
            processedConnectedMessage(json);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Connection closed");
        heartbeatTimer.cancel();
    }

    @Override
    public void onError(Exception e) {
        System.out.println("[WS] Error: " + e.getMessage());
        e.printStackTrace();
    }

    private void send(WebSocketMessage message) {
        send(message.toString());
    }

    private void processConnectingMessage(Json messageJson) {
        if (isMessageType(messageJson, OpCodes.HELLO)) {
            heartbeatTimer.scheduleAtFixedRate(new SendHeartbeatTask(), 0, messageJson.at("d").at("heartbeat_interval").asInteger());
            send(new IdentifyMessage());
        } else if (isMessageType(messageJson, OpCodes.HEARTBEAT_ACK)) {
            System.out.println("[WS] Received heartbeat acknowledgment");
        } else if (isMessageType(messageJson, OpCodes.READY)) {
            state = State.CONNECTED;
        }
    }

    private void processedConnectedMessage(Json messageJson) {

    }

    private static boolean isMessageType(Json messageJson, int opcode) {
        return hasMatchingAttribute(messageJson, "op", opcode);
    }

    private static boolean isMessageType(Json messageJson, String type) {
        boolean isOpZero = hasMatchingAttribute(messageJson, "op", 0);
        boolean isType = hasMatchingAttribute(messageJson, "t", type);
        return isOpZero && isType;
    }

    private static boolean hasMatchingAttribute(Json json, String key, int value) {
        return json.has(key) && json.at(key).isNumber() && json.at(key).asInteger() == value;
    }

    private static boolean hasMatchingAttribute(Json json, String key, String value) {
        return json.has(key) && json.at(key).isString() && json.at(key).asString().equals(value);
    }

    private class SendHeartbeatTask extends TimerTask {
        @Override
        public void run() {
            System.out.println("[WS] Sending heartbeat (" + heartbeatSequenceNumber + ")");
            send(new HeartbeatMessage(heartbeatSequenceNumber));
        }
    }
}
