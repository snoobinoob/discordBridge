package discordbridge.websocket;

import discordbridge.DiscordBot;
import discordbridge.Utils;
import discordbridge.websocket.message.HeartbeatMessage;
import discordbridge.websocket.message.IdentifyMessage;
import discordbridge.websocket.message.ResumeMessage;
import discordbridge.websocket.message.WebSocketMessage;
import mjson.Json;
import necesse.engine.GameLog;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DiscordWebSocketClient extends WebSocketClient {
    private enum State {
        TO_CONNECT,
        CONNECTING,
        TO_RECONNECT,
        RECONNECTING,
        CONNECTED,
    }

    private static final Set<Integer> disconnectCodesToIgnore = new HashSet<>();

    {
        disconnectCodesToIgnore.add(1000);
        disconnectCodesToIgnore.add(1001);
        disconnectCodesToIgnore.add(1012);
        disconnectCodesToIgnore.add(4004);
        disconnectCodesToIgnore.add(4010);
        disconnectCodesToIgnore.add(4011);
        disconnectCodesToIgnore.add(4012);
        disconnectCodesToIgnore.add(4013);
        disconnectCodesToIgnore.add(4014);
    }

    private State state;
    public Object heartbeatSequenceNumber;
    private Timer heartbeatTimer;
    public ReconnectData reconnectData;

    public DiscordWebSocketClient(URI websocketUri) {
        super(websocketUri);
        state = State.TO_CONNECT;
    }

    public DiscordWebSocketClient(ReconnectData reconnectData, Object heartbeatSequenceNumber) {
        super(reconnectData.gatewayUri);
        this.reconnectData = reconnectData;
        this.heartbeatSequenceNumber = heartbeatSequenceNumber;
        state = State.TO_RECONNECT;
    }

    @Override
    public void connect() {
        super.connect();
        state = state == State.TO_CONNECT ? State.CONNECTING : State.RECONNECTING;
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
        if (isMessageType(json, OpCodes.DISPATCH)) {
            heartbeatSequenceNumber = json.at("s").getValue();
            Utils.debug("[WS] Set sequence to: " + heartbeatSequenceNumber);
        }

        if (isMessageType(json, OpCodes.HEARTBEAT)) {
            send(new HeartbeatMessage(heartbeatSequenceNumber));
        } else if (isMessageType(json, OpCodes.RECONNECT)) {
            DiscordBot.reconnectWebSocket(reconnectData, heartbeatSequenceNumber);
        } else if (isMessageType(json, OpCodes.INVALID_SESSION)) {
            DiscordBot.reconnectWebSocket();
        } else if (state == State.CONNECTING) {
            processConnectingMessage(json);
        } else if (state == State.RECONNECTING) {
            processReconnectingMessage(json);
        } else if (state == State.CONNECTED) {
            processedConnectedMessage(json);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Utils.log(String.format("[WS] Connection closed (%d: %s)", code, reason));
        heartbeatTimer.cancel();
        if (!disconnectCodesToIgnore.contains(code)) {
            DiscordBot.reconnectWebSocket();
        }
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
            String sessionID = Utils.getStringOrNull(messageJson, "d.session_id");
            String resumeUrl = Utils.getStringOrNull(messageJson, "d.resume_gateway_url");
            reconnectData = new ReconnectData(sessionID, resumeUrl);
            state = State.CONNECTED;
        }
    }

    private void processReconnectingMessage(Json messageJson) {
        if (isMessageType(messageJson, OpCodes.HELLO)) {
            Json intervalJson = Utils.atPath(messageJson, "d.heartbeat_interval");
            if (intervalJson == null) {
                Utils.warn("[WS] Missing heartbeat interval in HELLO event");
                return;
            }
            heartbeatTimer.scheduleAtFixedRate(new SendHeartbeatTask(), 0, intervalJson.asInteger());
            Utils.debug("[WS] Sending Resume message");
            send(new ResumeMessage(reconnectData, heartbeatSequenceNumber));
        } else if (isMessageType(messageJson, OpCodes.RESUMED)) {
            Utils.log("[WS] Resume complete!");
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
        boolean isOpZero = Utils.hasMatchingAttribute(messageJson, "op", OpCodes.DISPATCH);
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
