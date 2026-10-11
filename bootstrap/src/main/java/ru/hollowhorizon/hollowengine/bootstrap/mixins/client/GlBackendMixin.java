package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.mojang.blaze3d.opengl.GlBackend;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(GlBackend.class)
public class GlBackendMixin {
    @Redirect(method = "setWindowHints", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirectGlfwWindowHint(int target, int value) {
        if (target == GLFW.GLFW_CONTEXT_VERSION_MAJOR || target == GLFW.GLFW_CONTEXT_VERSION_MINOR) {
            String versionText = BootstrapRuntimeManager.bridge().getOpenGlVersionOverride();
            if (versionText != null && versionText.contains(".")) {
                String[] version = versionText.split("\\.", 2);
                if (target == GLFW.GLFW_CONTEXT_VERSION_MAJOR) {
                    value = Integer.parseInt(version[0]);
                } else {
                    value = Integer.parseInt(version[1]);
                }
            }
        }
        GLFW.glfwWindowHint(target, value);
    }
}
