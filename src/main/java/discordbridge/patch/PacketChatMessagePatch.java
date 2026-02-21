package discordbridge.patch;

import discordbridge.DiscordBot;
import discordbridge.Utils;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import net.bytebuddy.asm.Advice;

public class PacketChatMessagePatch {
    @ModMethodPatch(target = PacketChatMessage.class, name = "processServer", arguments = {
            NetworkPacket.class,
            Server.class,
            ServerClient.class
    })
    public static class ProcessServerPatch {
        @Advice.OnMethodExit
        public static void onExit(@Advice.This PacketChatMessage thiz, @Advice.Argument(2) ServerClient client) {
            String message = thiz.gameMessage.translate();

            if (thiz.slot != client.slot || message.startsWith("/")) {
                return;
            }

            String author = client.getName();
            DiscordBot.sendChatMessage(Utils.getDiscordMessage(author, message));
        }
    }
}
