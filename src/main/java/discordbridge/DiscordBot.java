package discordbridge;

import com.intellectualsites.http.ContentType;
import com.intellectualsites.http.EntityMapper;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.HttpResponse;
import discordbridge.websocket.DiscordWebSocketClient;
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
            System.out.println("Error fetching self");
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
            System.out.println("Error fetching websocket url");
            return;
        }
        String websocketUrl = body.at("url").asString();
        try {
            wsClient = new DiscordWebSocketClient(new URI(websocketUrl));
            wsClient.connect();
        } catch (URISyntaxException e) {
            System.out.println("Unexpected format exception in websocket url");
        }
    }

    public static void handleChatMessage(Json data) {
        String authorID = Utils.getStringOrNull(data, "d.author.id");
        String channelID = Utils.getStringOrNull(data, "d.channel_id");
        if (instance.botUserID.equals(authorID) || !Settings.channelID.equals(channelID)) {
            return;
        }

        String author = Utils.getStringOrNull(data, "d.member.nick");
        if (author == null) {
            author = Utils.getStringOrNull(data, "d.author.global_name");
        }
        if (author == null) {
            author = Utils.getStringOrNull(data, "d.author.username");
        }
        String content = Utils.getStringOrNull(data, "d.content");

        String message = String.format("[Discord] %s: %s", author, content);

        instance.server.network.sendToAllClients(new PacketChatMessage(message));
    }

    public static void sendChatMessage(String author, String message) {
        Json data = Json.object()
                .set("content", String.format("[%s]: %s", author, message))
                .set("tts", false);
        instance.makeRequest("post", "/channels/" + Settings.channelID + "/messages", data);
    }

    private Json makeRequest(String method, String path) {
        return makeRequest(method, path, null);
    }

    private Json makeRequest(String method, String path, Json data) {
        HttpClient.WrappedRequestBuilder builder;
        if (method.equals("get")) {
            builder = httpClient.get(path);
        } else if (method.equals("post")) {
            builder = httpClient.post(path)
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

    public static void updatePresence() {
        if (instance.wsClient != null) {
            instance.wsClient.send(new PresenceMessage(instance.server));
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
