package tfar.morphproxy;

import me.ichun.mods.ichunutil.common.core.config.ConfigHandler;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = MorphProxy.MODID, name = MorphProxy.NAME, version = MorphProxy.VERSION,
        dependencies = "required-after:ichunutil@[" + iChunUtil.VERSION_MAJOR + ".2.0," + (iChunUtil.VERSION_MAJOR + 1) + ".0.0)"
)
public class MorphProxy {
    public static final String MODID = "morphproxy";
    public static final String NAME = "Morph Proxy";
    public static final String VERSION = "1.0";

    public static ModConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = ConfigHandler.registerConfig(new ModConfig(event.getSuggestedConfigurationFile()));
    }

    @Mod.EventHandler
    public void registerPacket(FMLInitializationEvent e) {
        PacketHandler.registerMessages(MODID);
    }

    @Mod.EventBusSubscriber(Side.CLIENT)
    public static class Client {
        @SubscribeEvent
        public static void registerKeybinds(ModelRegistryEvent e) {

        }
    }
}
