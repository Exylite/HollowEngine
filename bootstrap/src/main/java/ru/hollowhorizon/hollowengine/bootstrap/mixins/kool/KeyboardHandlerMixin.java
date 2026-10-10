package ru.hollowhorizon.hollowengine.bootstrap.mixins.kool;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long windowPointer, int action, KeyEvent event, CallbackInfo ci) {
        if (BootstrapRuntimeManager.bridge().onKeyboardKey(windowPointer, event.key(), event.scancode(), action, event.modifiers())) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onChar(long windowPointer, CharacterEvent event, CallbackInfo ci) {
        if (BootstrapRuntimeManager.bridge().onKeyboardChar(windowPointer, event.codepoint(), 0)) {
            ci.cancel();
        }
    }
}
