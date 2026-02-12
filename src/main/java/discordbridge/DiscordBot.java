package discordbridge;

public class DiscordBot implements Runnable {
    public void run() {
        System.out.println("Bot token is: " + Settings.token);
    }
}
