package discordbridge;

import com.intellectualsites.http.ContentType;
import com.intellectualsites.http.EntityMapper;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.HttpResponse;
import discordbridge.websocket.DiscordWebSocketClient;
import discordbridge.websocket.ReconnectData;
import discordbridge.websocket.message.PresenceMessage;
import mjson.Json;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

public class DiscordBot implements Runnable {
    public static final DiscordBot instance = new DiscordBot();

    private Server server;
    private final HttpClient httpClient;
    private DiscordWebSocketClient wsClient;
    private String botUserID;

    private DiscordBot() {
        EntityMapper mapper = EntityMapper.newInstance().registerDeserializer(Json.class, new JsonDeserializer());
        httpClient = HttpClient.newBuilder()
                .withBaseURL("https://discord.com/api/v10")
                .withEntityMapper(mapper)
                .build();
    }

    public static void init(Server server) {
        instance.server = server;
    }

    public void run() {
        Json body = makeRequest("get", "/users/@me");
        if (body == null) {
            Utils.warn("Error fetching bot information");
            return;
        }
        botUserID = Utils.getStringOrElse(body, "id", "");
        signIn();
    }

    public static void stop() {
        if (instance.wsClient != null) {
            instance.wsClient.close();
        }
    }

    private void signIn() {
        Json body = makeRequest("get", "/gateway/bot");
        if (body == null) {
            Utils.warn("Error fetching websocket url");
            return;
        }
        String websocketUrl = body.at("url").asString();
        try {
            wsClient = new DiscordWebSocketClient(new URI(websocketUrl));
            wsClient.connect();
        } catch (URISyntaxException e) {
            Utils.warn("Unexpected format exception in websocket url");
        }
    }

    public static void reconnectWebSocket() {
        instance.reconnect(null, null);
    }

    public static void reconnectWebSocket(ReconnectData data, Object sequenceNumber) {
        instance.reconnect(data, sequenceNumber);
    }

    private void reconnect(ReconnectData data, Object sequenceNumber) {
        if (wsClient != null && !wsClient.isClosed()) {
            wsClient.close(1012, "Reconnecting");
        }
        if (data == null) {
            signIn();
        } else {
            wsClient = new DiscordWebSocketClient(data, sequenceNumber);
            wsClient.connect();
        }
    }

    public static void handleChatMessage(Json messageJson) {
        String authorID = Utils.getStringOrNull(messageJson, "d.author.id");
        String channelID = Utils.getStringOrNull(messageJson, "d.channel_id");
        if (authorID.equals(instance.botUserID) || !Settings.channelID.equals(channelID)) {
            return;
        }

        String message = Utils.getNecesseMessage(messageJson);
        instance.server.network.sendToAllClients(new PacketChatMessage(message));
    }

    public static void sendChatMessage(String message) {
        Json data = Json.object()
                .set("content", message)
                .set("tts", false);
        makeRequest("post", "/channels/" + Settings.channelID + "/messages", data);
    }

    public static Json makeRequest(String method, String path) {
        return makeRequest(method, path, null);
    }

    private static Json makeRequest(String method, String path, Json data) {
        HttpClient.WrappedRequestBuilder builder;
        if (method.equals("get")) {
            builder = instance.httpClient.get(path);
        } else if (method.equals("post")) {
            builder = instance.httpClient.post(path)
                    .withHeader("Content-Type", "application/json")
                    .withInput(data::toString);
        } else {
            return null;
        }
        HttpResponse response = builder.withHeader("Authorization", "Bot " + Settings.token).execute();
        if (response == null) {
            return null;
        }
        return response.getResponseEntity(Json.class);
    }

    public static void updatePresence(int expectedDelta) {
        if (instance.wsClient != null) {
            instance.wsClient.send(new PresenceMessage(instance.server, expectedDelta));
        }
    }

    private static class JsonDeserializer implements EntityMapper.EntityDeserializer<Json> {
        @Override
        public @NotNull Json deserialize(@Nullable ContentType contentType, byte[] input) {
            String body = new String(input);
            return Json.read(body);
        }
    }
}
