package discordbridge.websocket;

public class OpCodes {
    public static final int HEARTBEAT = 1;
    public static final int IDENTIFY = 2;
    public static final int HELLO = 10;
    public static final int HEARTBEAT_ACK = 11;
    public static final String READY = "READY";
    public static final String MESSAGE_CREATE = "MESSAGE_CREATE";
}
