package tfar.morphproxy.clien;

import me.ichun.mods.ichunutil.client.core.event.RendererSafeCompatibilityEvent;
import me.ichun.mods.ichunutil.client.keybind.KeyBind;
import me.ichun.mods.ichunutil.client.keybind.KeyEvent;
import me.ichun.mods.ichunutil.client.render.RendererHelper;
import me.ichun.mods.ichunutil.common.core.util.EntityHelper;
import me.ichun.mods.morph.api.ability.Ability;
import me.ichun.mods.morph.client.model.ModelHandler;
import me.ichun.mods.morph.client.morph.MorphInfoClient;
import me.ichun.mods.morph.client.render.RenderPlayerHand;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphState;
import me.ichun.mods.morph.common.packet.PacketGuiInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import tfar.morphproxy.MorphProxy;

import java.util.*;

@Mod.EventBusSubscriber(Side.CLIENT)
public class MorphProxyEventHandlerClient {

    public static final int SELECTOR_SHOW_TIME = 10;
    public static final int SELECTOR_SCROLL_TIME = 3;

    public static final int RADIAL_SHOW_TIME = 3;

    public static HashMap<String, MorphInfoClient> morphsActive = new HashMap<>(); //Current morphs per-player

    public static RenderPlayerHand renderHandInstance;
    public static boolean allowSpecialRender;
    public static boolean forcePlayerRender;

    public static int abilityScroll;

    public static boolean selectorShow = false;
    public static int selectorShowTimer = 0;
    public static int selectorSelectedPrevVert = 0; //which morph category was selected
    public static int selectorSelectedPrevHori = 0; //which morph in category was selected
    public static int selectorSelectedVert = 0; //which morph category is selected
    public static int selectorSelectedHori = 0; //which morph in category is selected
    public static int selectorScrollVertTimer = 0;
    public static int selectorScrollHoriTimer = 0;

    public static boolean showFavourites = false;
    public static int radialShowTimer = 0;
    public static double radialDeltaX = 0D;
    public static double radialDeltaY = 0D;
    public static float radialPlayerYaw;
    public static float radialPlayerPitch;

    public static ArrayList<MorphState> favouriteStates = new ArrayList<>();


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRendererSafeCompatibility(RendererSafeCompatibilityEvent event) {
        for (Map.Entry<Class<? extends Entity>, Render<? extends Entity>> e : Minecraft.getMinecraft().getRenderManager().entityRenderMap.entrySet()) {
            Class clz = e.getKey();
            if (EntityLivingBase.class.isAssignableFrom(clz)) {
                ModelHandler.dissectForModels(clz, e.getValue());
            }
            ModelHandler.mapPlayerModels();
        }
    }

    @SubscribeEvent
    public static void onKeyEvent(KeyEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.keyBind.equals(MorphProxy.config.keySelectorUp1) || event.keyBind.equals(MorphProxy.config.keySelectorDown1) ||
                event.keyBind.equals(MorphProxy.config.keySelectorLeft1) || event.keyBind.equals(MorphProxy.config.keySelectorRight1)) {
            handleSelectorNavigation(event.keyBind);
        } else if (event.keyBind.equals(Morph.config.keySelectorSelect) || (event.keyBind.keyIndex == mc.gameSettings.keyBindAttack.getKeyCode() && event.keyBind.isMinecraftBind())) {
            if (selectorShow) {
                selectorShow = false;
                selectorShowTimer = MorphProxyEventHandlerClient.SELECTOR_SHOW_TIME - selectorShowTimer;
                selectorScrollHoriTimer = MorphProxyEventHandlerClient.SELECTOR_SCROLL_TIME;

                MorphState selectedState = getCurrentlySelectedMorphState();
                MorphInfoClient info = morphsActive.get(mc.player.getName());

                if (selectedState != null && (info != null && !info.nextState.currentVariant.equals(selectedState.currentVariant) || info == null && !selectedState.currentVariant.playerName.equalsIgnoreCase(mc.player.getName()))) {
                    //todo
                    Morph.channel.sendToServer(new PacketGuiInput(selectedState.currentVariant.thisVariant.identifier, 0, false));
                }
            } else if (showFavourites) {
                showFavourites = false;
                selectRadialMenu();
            }
        } else if (event.keyBind.equals(Morph.config.keySelectorCancel) || (event.keyBind.keyIndex == mc.gameSettings.keyBindUseItem.getKeyCode() && event.keyBind.isMinecraftBind())) {
            if (selectorShow) {
                if (mc.currentScreen instanceof GuiIngameMenu) {
                    mc.displayGuiScreen(null);
                }
                selectorShow = false;
                selectorShowTimer = MorphProxyEventHandlerClient.SELECTOR_SHOW_TIME - selectorShowTimer;
                selectorScrollHoriTimer = MorphProxyEventHandlerClient.SELECTOR_SCROLL_TIME;
            } else if (showFavourites) {
                showFavourites = false;
            }
        } else if (event.keyBind.equals(Morph.config.keySelectorRemoveMorph) || event.keyBind.keyIndex == Keyboard.KEY_DELETE) {
            if (selectorShow) {
                MorphState selectedState = getCurrentlySelectedMorphState();
                MorphInfoClient info = morphsActive.get(mc.player.getName());

                if (selectedState != null && !selectedState.currentVariant.thisVariant.isFavourite && ((info == null || !info.nextState.currentVariant.thisVariant.identifier.equalsIgnoreCase(selectedState.currentVariant.thisVariant.identifier)) && !selectedState.currentVariant.playerName.equalsIgnoreCase(mc.player.getName()))) {
                    //todo
                    Morph.channel.sendToServer(new PacketGuiInput(selectedState.currentVariant.thisVariant.identifier, 2, false));
                }
            }
        } else if (event.keyBind.equals(Morph.config.keyFavourite)) {
            if (selectorShow) {
                MorphState selectedState = getCurrentlySelectedMorphState();
                if (selectedState != null && !selectedState.currentVariant.playerName.equalsIgnoreCase(mc.player.getName())) {
                    selectedState.currentVariant.thisVariant.isFavourite = !selectedState.currentVariant.thisVariant.isFavourite;
                    //todo
                    Morph.channel.sendToServer(new PacketGuiInput(selectedState.currentVariant.thisVariant.identifier, 1, selectedState.currentVariant.thisVariant.isFavourite));
                    MorphState playerState = favouriteStates.get(0);
                    Morph.eventHandlerClient.favouriteStates.remove(0);
                    if (selectedState.currentVariant.thisVariant.isFavourite) {
                        if (!Morph.eventHandlerClient.favouriteStates.contains(selectedState)) {
                            favouriteStates.add(selectedState);
                            Collections.sort(favouriteStates);
                        }
                    } else {
                        favouriteStates.remove(selectedState);
                    }
                    favouriteStates.add(0, playerState);
                }
            } else if (mc.currentScreen == null) {
                showFavourites = true;
                radialShowTimer = RADIAL_SHOW_TIME;
                radialDeltaX = 0D;
                radialDeltaY = 0D;
                radialPlayerYaw = mc.player.rotationYaw;
                radialPlayerPitch = mc.player.rotationPitch;
            }
        }
        if (event.keyBind.equals(Morph.config.keyFavourite) && showFavourites) {
            //RADIAL MENU
            showFavourites = false;
            selectRadialMenu();
        }
    }

    @SubscribeEvent
    public static void onMouseEvent(MouseEvent event) {
        if (selectorShow) {
            int k = event.getDwheel();
            if (k != 0) {
                KeyBind bind;
                if (GuiScreen.isShiftKeyDown()) {
                    if (k > 0) {
                        bind = MorphProxy.config.keySelectorLeft1;
                    } else {
                        bind = MorphProxy.config.keySelectorRight1;
                    }
                } else {
                    if (k > 0) {
                        bind = MorphProxy.config.keySelectorUp1;
                    } else {
                        bind = MorphProxy.config.keySelectorDown1;
                    }
                }
                handleSelectorNavigation(bind);
                event.setCanceled(true);
            }
        } else if (showFavourites) {
            radialDeltaX += event.getDx() / 100D;
            radialDeltaY += event.getDy() / 100D;

            double mag = Math.sqrt(radialDeltaX * radialDeltaX + radialDeltaY * radialDeltaY);
            if (mag > 1.0D) {
                radialDeltaX /= mag;
                radialDeltaY /= mag;
            }
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote && event.getWorld() instanceof WorldClient) {
            //Clean up the Morph States and stuff like that to prevent mem leaks.
            morphsActive.values().forEach(MorphInfoClient::clean);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) {
            MorphInfo info = morphsActive.get(mc.player.getName());
            if (event.phase == TickEvent.Phase.START) {
                if (info != null && info.isMorphing()) {
                    float morphTransition = info.getMorphTransitionProgress(event.renderTickTime);

                    EntityLivingBase prevEnt = info.prevState.getEntInstance(mc.world);
                    EntityLivingBase nextEnt = info.nextState.getEntInstance(mc.world);

                    mc.player.eyeHeight = EntityHelper.interpolateValues(prevEnt.getEyeHeight(), nextEnt.getEyeHeight(), morphTransition);
                }

                if (showFavourites) {
                    Mouse.getDX();
                    Mouse.getDY();
                    mc.mouseHelper.deltaX = mc.mouseHelper.deltaY = 0;
//                    mc.renderViewEntity.prevRotationYawHead = mc.renderViewEntity.rotationYawHead = radialPlayerYaw;
//                    mc.renderViewEntity.prevRotationYaw = mc.renderViewEntity.rotationYaw = radialPlayerYaw;
//                    mc.renderViewEntity.prevRotationPitch = mc.renderViewEntity.rotationPitch = radialPlayerPitch;
                }
            } else {
                ScaledResolution reso = new ScaledResolution(mc);
                drawSelector(mc, reso, event.renderTickTime);
                drawRadialMenu(mc, reso, event.renderTickTime);

                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                for (Map.Entry<String, MorphInfoClient> e : morphsActive.entrySet()) {
                    MorphInfoClient morphInfo = e.getValue();
                    ArrayList<Ability> abilities = morphInfo.nextState.abilities;
                    if (abilities != null) {
                        for (Ability ability : abilities) {
                            ability.postRender();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null) {
                if (!mc.isGamePaused()) {
                    morphsActive.values().forEach(MorphInfo::tick);
                    for (Map.Entry<String, ArrayList<MorphState>> e : Morph.eventHandlerClient.playerMorphs.entrySet()) {
                        for (MorphState state : e.getValue()) {
                            state.getEntInstance(mc.world).ticksExisted++;
                        }
                    }
                }

                if (mc.currentScreen != null) {
                    if (selectorShow) {
                        if (mc.currentScreen instanceof GuiIngameMenu) {
                            mc.displayGuiScreen(null);
                        }
                        selectorShow = false;
                        selectorShowTimer = SELECTOR_SHOW_TIME - selectorShowTimer;
                        selectorScrollHoriTimer = SELECTOR_SCROLL_TIME;
                    }
                }
                abilityScroll++;
                if (selectorShowTimer > 0) {
                    selectorShowTimer--;
                }
                if (radialShowTimer > 0) {
                    radialShowTimer--;
                }
                selectorScrollVertTimer--;
                selectorScrollHoriTimer--;
            }
        }
    }

    public static void selectRadialMenu() {
        double mag = Math.sqrt(radialDeltaX * radialDeltaX + radialDeltaY * radialDeltaY);
        double magAcceptance = 0.8D;

        double radialAngle = -720F;
        if (mag > magAcceptance) {
            //is on radial menu
            //TODO atan2?
            double aSin = Math.toDegrees(Math.asin(radialDeltaX));
            if (radialDeltaY >= 0 && radialDeltaX >= 0) {
                radialAngle = aSin;
            } else if (radialDeltaY < 0 && radialDeltaX >= 0) {
                radialAngle = 90D + (90D - aSin);
            } else if (radialDeltaY < 0 && radialDeltaX < 0) {
                radialAngle = 180D - aSin;
            } else if (radialDeltaY >= 0 && radialDeltaX < 0) {
                radialAngle = 270D + (90D + aSin);
            }
        } else {
            return;
        }

        if (mag > 0.9999999D) {
            mag = Math.round(mag);
        }

        for (int i = 0; i < Morph.eventHandlerClient.favouriteStates.size(); i++) {
            float leeway = 360F / Morph.eventHandlerClient.favouriteStates.size();
            if (mag > magAcceptance * 0.75D && (i == 0 && (radialAngle < (leeway / 2) && radialAngle >= 0F || radialAngle > (360F) - (leeway / 2)) || i != 0 && radialAngle < (leeway * i) + (leeway / 2) && radialAngle > (leeway * i) - (leeway / 2))) {
                MorphState selectedState = Morph.eventHandlerClient.favouriteStates.get(i);
                MorphInfoClient info = morphsActive.get(Minecraft.getMinecraft().player.getName());

                if (selectedState != null && (info != null && !info.nextState.currentVariant.equals(selectedState.currentVariant) || info == null && !selectedState.currentVariant.playerName.equalsIgnoreCase(Minecraft.getMinecraft().player.getName()))) {
                    Morph.channel.sendToServer(new PacketGuiInput(selectedState.currentVariant.thisVariant.identifier, 0, false));
                    break;
                }
            }
        }

    }

    public static void drawRadialMenu(Minecraft mc, ScaledResolution reso, float renderTick) {
        if ((radialShowTimer > 0 || showFavourites) && !mc.gameSettings.hideGUI) {
            double mag = Math.sqrt(radialDeltaX * radialDeltaX + radialDeltaY * radialDeltaY);
            double magAcceptance = 0.8D;

            float prog = 1.0F - (radialShowTimer - renderTick) / (float) RADIAL_SHOW_TIME;
            if (prog > 1.0F) {
                prog = 1.0F;
            }

            int radius = 80;
            radius *= Math.pow(prog, 0.5D);

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();

            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();

            int NUM_PIZZA_SLICES = 100;

            float zLev = -0.05F;

            GlStateManager.disableTexture2D();

            float rad;

            int stencilBit = -1;
            if (RendererHelper.canUseStencils()) {
                stencilBit = MinecraftForgeClient.reserveStencilBit();
                if (stencilBit >= 0) {
                    GL11.glEnable(GL11.GL_STENCIL_TEST);
                    GlStateManager.depthMask(false);
                    GlStateManager.colorMask(false, false, false, false);

                    final int stencilMask = 1 << stencilBit;

                    GL11.glStencilMask(stencilMask);
                    GL11.glStencilFunc(GL11.GL_ALWAYS, stencilMask, stencilMask);
                    GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                    GL11.glClearStencil(0);
                    GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT);

                    rad = (mag > magAcceptance ? 0.85F : 0.82F) * prog * (257F / (float) reso.getScaledHeight());

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    GlStateManager.glBegin(GL11.GL_TRIANGLE_FAN);
                    GlStateManager.glVertex3f(0, 0, zLev);
                    for (int i = 0; i <= NUM_PIZZA_SLICES; i++) { //NUM_PIZZA_SLICES decides how round the circle looks.
                        double angle = Math.PI * 2 * i / NUM_PIZZA_SLICES;
                        GlStateManager.glVertex3f((float) (Math.cos(angle) * reso.getScaledHeight_double() / reso.getScaledWidth_double() * rad), (float) (Math.sin(angle) * rad), zLev);
                    }
                    GlStateManager.glEnd();

                    GL11.glStencilFunc(GL11.GL_ALWAYS, 0, stencilMask);

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    rad = 0.44F * prog * (257F / (float) reso.getScaledHeight());

                    GlStateManager.glBegin(GL11.GL_TRIANGLE_FAN);
                    GlStateManager.glVertex3f(0, 0, zLev);
                    for (int i = 0; i <= NUM_PIZZA_SLICES; i++) { //NUM_PIZZA_SLICES decides how round the circle looks.
                        double angle = Math.PI * 2 * i / NUM_PIZZA_SLICES;
                        GlStateManager.glVertex3f((float) (Math.cos(angle) * reso.getScaledHeight_double() / reso.getScaledWidth_double() * rad), (float) (Math.sin(angle) * rad), zLev);
                    }
                    GlStateManager.glEnd();

                    GL11.glStencilMask(0x00);
                    GL11.glStencilFunc(GL11.GL_EQUAL, stencilMask, stencilMask);

                    GlStateManager.depthMask(true);
                    GlStateManager.colorMask(true, true, true, true);
                }
            }

            rad = (mag > magAcceptance ? 0.85F : 0.82F) * prog * (257F / (float) reso.getScaledHeight());

            GlStateManager.color(0.0F, 0.0F, 0.0F, mag > magAcceptance ? 0.6F : 0.4F);

            GlStateManager.glBegin(GL11.GL_TRIANGLE_FAN);
            GlStateManager.glVertex3f(0, 0, zLev);
            for (int i = 0; i <= NUM_PIZZA_SLICES; i++) { //NUM_PIZZA_SLICES decides how round the circle looks.
                double angle = Math.PI * 2 * i / NUM_PIZZA_SLICES;
                GlStateManager.glVertex3f((float) (Math.cos(angle) * reso.getScaledHeight_double() / reso.getScaledWidth_double() * rad), (float) (Math.sin(angle) * rad), zLev);
            }
            GlStateManager.glEnd();

            if (RendererHelper.canUseStencils() && stencilBit >= 0) {
                GL11.glDisable(GL11.GL_STENCIL_TEST);

                MinecraftForgeClient.releaseStencilBit(stencilBit);
            }

            GlStateManager.enableTexture2D();

            GlStateManager.popMatrix();

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);

            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();

            double radialAngle = -720F;
            if (mag > magAcceptance) {
                //is on radial menu
                double aSin = Math.toDegrees(Math.asin(radialDeltaX));

                if (radialDeltaY >= 0 && radialDeltaX >= 0) {
                    radialAngle = aSin;
                } else if (radialDeltaY < 0 && radialDeltaX >= 0) {
                    radialAngle = 90D + (90D - aSin);
                } else if (radialDeltaY < 0 && radialDeltaX < 0) {
                    radialAngle = 180D - aSin;
                } else if (radialDeltaY >= 0 && radialDeltaX < 0) {
                    radialAngle = 270D + (90D + aSin);
                }
            }

            if (mag > 0.9999999D) {
                mag = Math.round(mag);
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();

            for (int i = 0; i < Morph.eventHandlerClient.favouriteStates.size(); i++) {
                double angle = Math.PI * 2 * i / Morph.eventHandlerClient.favouriteStates.size();

                angle -= Math.toRadians(90D);

                float leeway = 360F / Morph.eventHandlerClient.favouriteStates.size();

                boolean selected = false;

                if (mag > magAcceptance * 0.75D && (i == 0 && (radialAngle < (leeway / 2) && radialAngle >= 0F || radialAngle > (360F) - (leeway / 2)) || i != 0 && radialAngle < (leeway * i) + (leeway / 2) && radialAngle > (leeway * i) - (leeway / 2))) {
                    selected = true;
                }

                float entSize = Math.max(Morph.eventHandlerClient.favouriteStates.get(i).getEntInstance(mc.world).width, Morph.eventHandlerClient.favouriteStates.get(i).getEntInstance(mc.world).height);

                float scaleMag = entSize > 2.5F ? (float) ((2.5F + (entSize - 2.5F) * (mag > magAcceptance && selected ? ((mag - magAcceptance) / (1.0F - magAcceptance)) : 0.0F)) / entSize) : 1.0F;

                Morph.eventHandlerClient.drawEntityOnScreen(Morph.eventHandlerClient.favouriteStates.get(i), Morph.eventHandlerClient.favouriteStates.get(i).getEntInstance(mc.world), reso.getScaledWidth() / 2 + (int) (radius * Math.cos(angle)), (reso.getScaledHeight() + 24) / 2 + (int) (radius * Math.sin(angle)), 16 * prog * scaleMag + (float) (selected ? 6 * mag : 0), 2, 2, renderTick, selected, true);
            }

            GlStateManager.popMatrix();
        }
    }

    public static void drawSelector(Minecraft mc, ScaledResolution reso, float renderTick) {
        if ((selectorShowTimer > 0 || selectorShow) && !mc.gameSettings.hideGUI) {
            if (selectorSelectedVert < 0) {
                selectorSelectedVert = 0;
            }
            while (selectorSelectedVert > Morph.eventHandlerClient.playerMorphs.size() - 1) {
                selectorSelectedVert--;
            }

            GlStateManager.pushMatrix();

            float progress = MathHelper.clamp((SELECTOR_SHOW_TIME - (selectorShowTimer - renderTick)) / (float) SELECTOR_SHOW_TIME, 0F, 1F);

            if (selectorShow) {
                progress = 1.0F - progress;
            }

            if (selectorShow && selectorShowTimer < 0) {
                progress = 0.0F;
            }

            progress = (float) Math.pow(progress, 2D);

            GlStateManager.translate(-52F * progress, 0.0F, 0.0F);

            int gap = (reso.getScaledHeight() - (42 * 5)) / 2;

            double size = 42D;
            double width1 = 0.0D;

            GlStateManager.pushMatrix();

            int maxShowable = (int) Math.ceil((double) reso.getScaledHeight() / size) + 2;

            if (selectorSelectedVert == 0 && selectorSelectedPrevVert > 0 || selectorSelectedPrevVert == 0 && selectorSelectedVert > 0) {
                maxShowable = 150;
            }

            float progressV = (SELECTOR_SCROLL_TIME - (selectorScrollVertTimer - renderTick)) / (float) SELECTOR_SCROLL_TIME;

            progressV = (float) Math.pow(progressV, 2D);

            if (progressV > 1.0F) {
                progressV = 1.0F;
                selectorSelectedPrevVert = selectorSelectedVert;
            }

            float progressH = (SELECTOR_SCROLL_TIME - (selectorScrollHoriTimer - renderTick)) / (float) SELECTOR_SCROLL_TIME;

            progressH = (float) Math.pow(progressH, 2D);

            if (progressH > 1.0F) {
                progressH = 1.0F;
                selectorSelectedPrevHori = selectorSelectedHori;
            }

            GlStateManager.translate(0.0F, ((selectorSelectedVert - selectorSelectedPrevVert) * 42F) * (1.0F - progressV), 0.0F);

            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableAlpha();

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            int i = 0;

            Iterator<Map.Entry<String, ArrayList<MorphState>>> ite = Morph.eventHandlerClient.playerMorphs.entrySet().iterator();

            while (ite.hasNext()) {
                Map.Entry<String, ArrayList<MorphState>> e = ite.next();

                if (i > selectorSelectedVert + maxShowable || i < selectorSelectedVert - maxShowable) {
                    i++;
                    continue;
                }

                double height1 = gap + size * (i - selectorSelectedVert);

                ArrayList<MorphState> states = e.getValue();
                if (states == null || states.isEmpty()) {
                    ite.remove();
                    i++;
                    break;
                }

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();

                if (i == selectorSelectedVert) {
                    if (selectorSelectedHori < 0) {
                        selectorSelectedHori = states.size() - 1;
                    }
                    if (selectorSelectedHori >= states.size()) {
                        selectorSelectedHori = 0;
                    }

                    boolean newSlide = false;

                    if (progressV < 1.0F && selectorSelectedPrevVert != selectorSelectedVert) {
                        selectorSelectedPrevHori = states.size() - 1;
                        selectorSelectedHori = 0;
                        newSlide = true;
                    }
                    if (!selectorShow) {
                        selectorSelectedHori = states.size() - 1;
                        newSlide = true;
                    } else if (progress > 0.0F) {
                        selectorSelectedPrevHori = states.size() - 1;
                        newSlide = true;
                    }

                    for (int j = 0; j < states.size(); j++) {
                        GlStateManager.pushMatrix();

                        GlStateManager.translate(newSlide && j == 0 ? 0.0D : ((selectorSelectedHori - selectorSelectedPrevHori) * 42F) * (1.0F - progressH), 0.0D, 0.0D);

                        mc.getTextureManager().bindTexture(states.size() == 1 || j == states.size() - 1 ? Morph.eventHandlerClient.rlUnselected : Morph.eventHandlerClient.rlUnselectedSide);

                        double dist = size * (j - selectorSelectedHori);

                        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                        bufferbuilder.pos(width1 + dist, height1 + size, -90.0D + j).tex(0.0D, 1.0D).endVertex();
                        bufferbuilder.pos(width1 + dist + size, height1 + size, -90.0D + j).tex(1.0D, 1.0D).endVertex();
                        bufferbuilder.pos(width1 + dist + size, height1, -90.0D + j).tex(1.0D, 0.0D).endVertex();
                        bufferbuilder.pos(width1 + dist, height1, -90.0D + j).tex(0.0D, 0.0D).endVertex();
                        tessellator.draw();

                        GlStateManager.popMatrix();
                    }
                } else {
                    mc.getTextureManager().bindTexture(Morph.eventHandlerClient.rlUnselected);
                    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                    bufferbuilder.pos(width1, height1 + size, -90.0D).tex(0.0D, 1.0D).endVertex();
                    bufferbuilder.pos(width1 + size, height1 + size, -90.0D).tex(1.0D, 1.0D).endVertex();
                    bufferbuilder.pos(width1 + size, height1, -90.0D).tex(1.0D, 0.0D).endVertex();
                    bufferbuilder.pos(width1, height1, -90.0D).tex(0.0D, 0.0D).endVertex();
                    tessellator.draw();
                }
                i++;
            }

            GlStateManager.disableBlend();

            int height1;

            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();

            gap += 36;

            i = 0;

            ite = Morph.eventHandlerClient.playerMorphs.entrySet().iterator();

            while (ite.hasNext()) {
                Map.Entry<String, ArrayList<MorphState>> e = ite.next();

                if (i > selectorSelectedVert + maxShowable || i < selectorSelectedVert - maxShowable) {
                    i++;
                    continue;
                }

                height1 = gap + (int) size * (i - selectorSelectedVert);

                ArrayList<MorphState> states = e.getValue();

                if (i == selectorSelectedVert) {
                    boolean newSlide = false;

                    if (progressV < 1.0F && selectorSelectedPrevVert != selectorSelectedVert) {
                        selectorSelectedPrevHori = states.size() - 1;
                        selectorSelectedHori = 0;
                        newSlide = true;
                    }
                    if (!selectorShow) {
                        selectorSelectedHori = states.size() - 1;
                        newSlide = true;
                    }

                    for (int j = 0; j < states.size(); j++) {
                        MorphState state = states.get(j);
                        GlStateManager.pushMatrix();

                        double dist = size * (j - selectorSelectedHori);
                        GlStateManager.translate((newSlide && j == 0 ? 0.0D : ((selectorSelectedHori - selectorSelectedPrevHori) * 42F) * (1.0F - progressH)) + dist, 0.0D, 0.0D);

                        EntityLivingBase entInstance = state.getEntInstance(mc.world);
                        float entSize = Math.max(entInstance.width, entInstance.height);
                        float prog = MathHelper.clamp(j - selectorSelectedHori == 0 ? (!selectorShow ? selectorScrollHoriTimer - renderTick : (3F - selectorScrollHoriTimer + renderTick)) / 3F : 0.0F, 0.0F, 1.0F);
                        float scaleMag = ((2.5F + (entSize - 2.5F) * prog) / entSize);

                        Morph.eventHandlerClient.drawEntityOnScreen(state, entInstance, 20, height1, entSize > 2.5F ? 16F * scaleMag : 16F, 2, 2, renderTick, true, j == states.size() - 1);

                        GlStateManager.popMatrix();
                    }
                } else {
                    MorphState state = states.get(0);
                    EntityLivingBase entInstance = state.getEntInstance(mc.world);
                    float entSize = Math.max(entInstance.width, entInstance.height);
                    float scaleMag = (2.5F / entSize);
                    Morph.eventHandlerClient.drawEntityOnScreen(state, entInstance, 20, height1, entSize > 2.5F ? 16F * scaleMag : 16F, 2, 2, renderTick, selectorSelectedVert == i, true);
                }
                GlStateManager.translate(0.0F, 0.0F, 20F);
                GlStateManager.color(1F, 1F, 1F, 1F);
                i++;
            }
            GlStateManager.popMatrix();

            if (selectorShow) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                gap -= 36;
                height1 = gap;

                mc.getTextureManager().bindTexture(Morph.eventHandlerClient.rlSelected);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();

                bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                bufferbuilder.pos(width1, height1 + size, -90.0D).tex(0.0D, 1.0D).endVertex();
                bufferbuilder.pos(width1 + size, height1 + size, -90.0D).tex(1.0D, 1.0D).endVertex();
                bufferbuilder.pos(width1 + size, height1, -90.0D).tex(1.0D, 0.0D).endVertex();
                bufferbuilder.pos(width1, height1, -90.0D).tex(0.0D, 0.0D).endVertex();
                tessellator.draw();

                GlStateManager.disableBlend();
            }
            GlStateManager.popMatrix();
        }
    }

    public static void handleSelectorNavigation(KeyBind bind) {
        Minecraft mc = Minecraft.getMinecraft();
        abilityScroll = 0;
        if(!selectorShow && mc.currentScreen == null) //show the selector.
        {
            selectorShow = true;
            selectorShowTimer = SELECTOR_SHOW_TIME - selectorShowTimer;
            selectorScrollVertTimer = selectorScrollHoriTimer = SELECTOR_SCROLL_TIME;
            selectorSelectedVert = selectorSelectedHori = 0; //Reset the selected selector position

            MorphInfoClient info = morphsActive.get(mc.player.getName());
            if(info != null)
            {
                String entName = info.nextState.getEntInstance(mc.world).getName();

                int i = 0;
                for (Map.Entry<String, ArrayList<MorphState>> e : Morph.eventHandlerClient.playerMorphs.entrySet()) {
                    if (e.getKey().equalsIgnoreCase(entName)) {
                        selectorSelectedVert = i;
                        ArrayList<MorphState> states = e.getValue();

                        for (int j = 0; j < states.size(); j++) {
                            if (states.get(j).currentVariant.equals(info.nextState.currentVariant)) {
                                selectorSelectedHori = j;
                                break;
                            }
                        }

                        break;
                    }
                    i++;
                }
            }
        }
        else if(bind.equals(MorphProxy.config.keySelectorUp1) || bind.equals(MorphProxy.config.keySelectorDown1)) //Vertical scrolling
        {
            selectorSelectedHori = 0;
            selectorSelectedPrevVert = selectorSelectedVert;
            selectorScrollHoriTimer = selectorScrollVertTimer = SELECTOR_SCROLL_TIME;

            if(bind.equals(MorphProxy.config.keySelectorUp1))
            {
                selectorSelectedVert--;
                if(selectorSelectedVert < 0)
                {
                    selectorSelectedVert = Morph.eventHandlerClient.playerMorphs.size() - 1;
                }
            }
            else
            {
                selectorSelectedVert++;
                if(selectorSelectedVert > Morph.eventHandlerClient.playerMorphs.size() - 1)
                {
                    selectorSelectedVert = 0;
                }
            }
        }
        else //Horizontal scrolling
        {
            selectorSelectedPrevHori = selectorSelectedHori;
            selectorScrollHoriTimer = SELECTOR_SCROLL_TIME;

            if(bind.equals(MorphProxy.config.keySelectorLeft1))
            {
                selectorSelectedHori--;
            }
            else
            {
                selectorSelectedHori++;
            }

            int i = 0;
            for (Map.Entry<String, ArrayList<MorphState>> e : Morph.eventHandlerClient.playerMorphs.entrySet()) {
                if (i == selectorSelectedVert) {
                    ArrayList<MorphState> states = e.getValue();
                    if (selectorSelectedHori < 0) {
                        selectorSelectedHori = states.size() - 1;
                    }
                    if (selectorSelectedHori >= states.size()) {
                        selectorSelectedHori = 0;
                    }
                    break;
                }
                i++;
            }
        }
    }

    public static MorphState getCurrentlySelectedMorphState() {
        int i = 0;
        for (Map.Entry<String, ArrayList<MorphState>> e : Morph.eventHandlerClient.playerMorphs.entrySet()) {
            if (i == selectorSelectedVert) {
                ArrayList<MorphState> states = e.getValue();
                if (selectorSelectedHori > states.size()) {
                    return null;
                } else {
                    return states.get(selectorSelectedHori);
                }
            }
            i++;
        }
        return null;
    }
}
