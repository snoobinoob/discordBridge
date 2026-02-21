package discordbridge.websocket;

import discordbridge.Utils;

import java.net.URI;
import java.net.URISyntaxException;

public class ReconnectData {
    public final String sessionID;
    public final URI gatewayUri;

    public ReconnectData(String sessionID, String gatewayUrl) {
        this.sessionID = sessionID;
        gatewayUri = safeUri(gatewayUrl);
    }

    private static URI safeUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            Utils.warn("Invalid URI format: '" + url + "'");
            return null;
        }
    }
}
