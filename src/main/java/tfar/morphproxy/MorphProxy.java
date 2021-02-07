package tfar.morphproxy;

import me.ichun.mods.ichunutil.common.core.config.ConfigHandler;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;

import java.util.HashSet;
import java.util.UUID;

@Mod(modid = MorphProxy.MODID, name = MorphProxy.NAME, version = MorphProxy.VERSION,
        dependencies = "required-after:ichunutil@[" + iChunUtil.VERSION_MAJOR + ".2.0," + (iChunUtil.VERSION_MAJOR + 1) + ".0.0)"
)
@Mod.EventBusSubscriber
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

    public static HashSet<UUID> alreadyJoined = new HashSet<>();

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            if (!alreadyJoined.contains(e.player.getGameProfile().getId()) && !e.player.world.isRemote) {
                EntityPlayerMP playerMP = (EntityPlayerMP) e.player;
                ForgeRegistry<EntityEntry> entityRegistry = GameData.getEntityRegistry();
                for (EntityEntry entityEntry : entityRegistry) {
                    Entity entity = entityEntry.newInstance(playerMP.world);
                    if (entity instanceof EntityLivingBase) {
                        MorphApi.getApiImpl().acquireMorph(playerMP, (EntityLivingBase) entity, false, false);
                    }
                }
                alreadyJoined.add(e.player.getGameProfile().getId());
            }
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
    }
}
