package tfar.morphproxy;

import me.ichun.mods.ichunutil.client.keybind.KeyBind;
import me.ichun.mods.ichunutil.common.core.config.ConfigBase;
import me.ichun.mods.ichunutil.common.core.config.annotations.ConfigProp;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.lang.reflect.Field;

public class ModConfig extends ConfigBase {

    public ModConfig(File file) {
        super(file);
    }

    @Override
    public String getModId() {
        return MorphProxy.MODID;
    }

    @Override
    public String getModName() {
        return MorphProxy.NAME;
    }

    @Override
    public void readProperty(Field field, boolean write) {
        super.readProperty(field, write);
    }

    @Override
    public void readFields(boolean write) {
        super.readFields(write);
    }

    @ConfigProp(category = "clientOnly", side = Side.CLIENT)
    public KeyBind keySelectorUp1 = new KeyBind(Keyboard.KEY_O);

    @ConfigProp(category = "clientOnly", side = Side.CLIENT)
    public KeyBind keySelectorDown1 = new KeyBind(Keyboard.KEY_P);

    @ConfigProp(category = "clientOnly", side = Side.CLIENT)
    public KeyBind keySelectorLeft1 = new KeyBind(Keyboard.KEY_O, true, false, false, false);

    @ConfigProp(category = "clientOnly", side = Side.CLIENT)
    public KeyBind keySelectorRight1 = new KeyBind(Keyboard.KEY_P, true, false, false, false);

}
