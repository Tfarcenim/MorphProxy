package tfar.morphproxy;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import tfar.morphproxy.network.C2SSendMorphPacket;

public class PacketHandler {

  public static SimpleNetworkWrapper INSTANCE = null;

  public PacketHandler() {
  }

  public static void registerMessages(String channelName) {
    INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(channelName);
    registerMessages();
  }

  public static int id;

  public static void registerMessages() {
    // Register messages which are sent from the client to the server here:
    INSTANCE.registerMessage(C2SSendMorphPacket.Handler.class, C2SSendMorphPacket.class, id++, Side.SERVER);
  }
}