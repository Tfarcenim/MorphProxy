package tfar.morphproxy;

import io.netty.buffer.ByteBuf;
import me.ichun.mods.morph.api.IApi;
import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

// not threadsafe!
public class MorphProxyPacket implements IMessage {

  public MorphProxyPacket() {
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    //recipe = CraftingManager.REGISTRY.getObjectById(buf.readInt());
  }

  @Override
  public void toBytes(ByteBuf buf) {
    //buf.writeInt(CraftingManager.REGISTRY.getIDForObject(recipe));
  }

  public static class Handler implements IMessageHandler<MorphProxyPacket, IMessage> {
    @Override
    public IMessage onMessage(MorphProxyPacket message, MessageContext ctx) {
      // Always use a construct like this to actually handle your message. This ensures that
      // youre 'handle' code is run on the main Minecraft thread. 'onMessage' itself
      // is called on the networking thread so it is not safe to do a lot of things
      // here.
      FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
      return null;
    }

    private void handle(MorphProxyPacket message, MessageContext ctx) {
      // This code is run on the server side. So you can do server-side calculations here
      EntityPlayerMP player = ctx.getServerHandler().player;
      List<EntityPlayerMP> playerMPS = player.getServer().getPlayerList().getPlayers();
      playerMPS.remove(player);
      if (!playerMPS.isEmpty()){
        EntityPlayerMP other = playerMPS.get(0);
        IApi morphApi = MorphApi.getApiImpl();
        //morphApi.forceMorph()
      }
    }
  }
}