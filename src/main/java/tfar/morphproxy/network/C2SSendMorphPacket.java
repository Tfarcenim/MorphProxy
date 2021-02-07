package tfar.morphproxy.network;

import io.netty.buffer.ByteBuf;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.handler.PlayerMorphHandler;
import me.ichun.mods.morph.common.morph.MorphVariant;
import me.ichun.mods.morph.common.packet.PacketUpdateMorphList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;

// not threadsafe!
public class C2SSendMorphPacket implements IMessage {

  public String identifier;
  public int id;
  public boolean flag;

  public C2SSendMorphPacket() {
  }

  public C2SSendMorphPacket(String ident, int id, boolean flag) {
    this.identifier = ident;
    this.id = id;
    this.flag = flag;
  }

  public void toBytes(ByteBuf buffer) {
    ByteBufUtils.writeUTF8String(buffer, this.identifier);
    buffer.writeInt(this.id);
    buffer.writeBoolean(this.flag);
  }

  public void fromBytes(ByteBuf buffer) {
    this.identifier = ByteBufUtils.readUTF8String(buffer);
    this.id = buffer.readInt();
    this.flag = buffer.readBoolean();
  }

  public static class Handler implements IMessageHandler<C2SSendMorphPacket, IMessage> {
    @Override
    public IMessage onMessage(C2SSendMorphPacket message, MessageContext ctx) {
      // Always use a construct like this to actually handle your message. This ensures that
      // youre 'handle' code is run on the main Minecraft thread. 'onMessage' itself
      // is called on the networking thread so it is not safe to do a lot of things
      // here.
      FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
      return null;
    }

    private void handle(C2SSendMorphPacket message, MessageContext ctx) {
      // This code is run on the server side. So you can do server-side calculations here
      EntityPlayerMP playerMP = ctx.getServerHandler().player;
      ArrayList<MorphVariant> morphs = Morph.eventHandlerServer.getPlayerMorphs(playerMP);
      boolean found = false;

      EntityPlayerMP otherPlayer = playerMP.getServer().getPlayerList().getPlayers().stream().filter(playerMP1 -> playerMP1 != playerMP).findFirst().orElse(null);

      if (otherPlayer != null) {
        for (int i = morphs.size() - 1; i >= 0; --i) {
          MorphVariant variant = morphs.get(i);
          MorphVariant.Variant var = variant.getVariantByIdentifier(message.identifier);
          if (var != null) {
            found = true;
            switch (message.id) {
              case 0:
                PlayerMorphHandler.getInstance().morphPlayer(otherPlayer, variant.createWithVariant(var));
                break;
              case 1:
                var.isFavourite = message.flag;
                break;
              case 2:
                found = false;
                if (variant.deleteVariant(var)) {
                  morphs.remove(i);
                }
            }

            PlayerMorphHandler.getInstance().savePlayerData(otherPlayer);
            break;
          }
        }

        if (!found) {
          Morph.channel.sendTo(new PacketUpdateMorphList(true, morphs.toArray(new MorphVariant[0])), otherPlayer);
        }
      }
    }
  }
}