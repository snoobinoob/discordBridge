package discordbridge;

import com.intellectualsites.http.EntityMapper;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.HttpResponse;
import discordbridge.websocket.DiscordWebSocketClient;
import mjson.Json;

import java.net.URI;
import java.net.URISyntaxException;

public class DiscordBot implements Runnable {
    private HttpClient httpClient;
    private DiscordWebSocketClient wsClient;

    public DiscordBot() {
//        EntityMapper mapper = EntityMapper.newInstance().registerDeserializer(JsonObject.class, GsonMapper.deserializer(JsonObject.class, GSON));
        EntityMapper mapper = EntityMapper.newInstance();
        httpClient = HttpClient.newBuilder()
                .withBaseURL("https://discord.com/api/v10")
                .withEntityMapper(mapper)
                .build();
    }

    public void run() {
        signIn();
    }

    public void stop() {
        wsClient.close();
    }

    private void signIn() {
        HttpResponse response = httpClient.get("/gateway/bot").withHeader("Authorization", "Bot " + Settings.token).execute();
        if (response == null) {
            System.out.println("Error fetching websocket url");
            return;
        }
        String body = response.getResponseEntity(String.class);
        String websocketUrl = Json.read(body).at("url").asString();
        try {
            wsClient = new DiscordWebSocketClient(new URI(websocketUrl));
            wsClient.connect();
        } catch (URISyntaxException e) {
            System.out.println("Unexpected format exception in websocket url");
        }
    }

    public void sendMessage(Json message) {
        wsClient.send(message.toString());
    }
}
