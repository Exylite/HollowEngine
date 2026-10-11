package ru.hollowhorizon.hollowengine.bootstrap.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(SkyRenderer.class)
public class LevelRendererMixin {
    @ModifyConstant(method = "renderSun", constant = @Constant(floatValue = 30.0F))
    private float hollowengine$changeSunSize(float original) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? original : BootstrapRuntimeManager.bridge().getSkySunSize(level, original);
    }

    @ModifyConstant(method = "renderMoon", constant = @Constant(floatValue = 20.0F))
    private float hollowengine$changeMoonSize(float original) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? original : BootstrapRuntimeManager.bridge().getSkyMoonSize(level, original);
    }
}
