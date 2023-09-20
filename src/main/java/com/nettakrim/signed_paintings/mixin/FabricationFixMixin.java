package com.nettakrim.signed_paintings.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class FabricationFixMixin {
    @Shadow
    private SelectionManager selectionManager;

    @Inject(at = @At("HEAD"), method = "renderSignText")
    private void renderSignText(DrawContext context, CallbackInfo ci) {
        // fabrication's multiline paste fix causes selection start and ends to get messed up for some reason
        // simply setting the selection makes it automatically clamp
        // https://falsehoodmc.github.io/#fixes.multiline_sign_paste
        // https://github.com/FalsehoodMC/Fabrication/tree/3.0/1.18/src/main/java/com/unascribed/fabrication/mixin/a_fixes/multiline_sign_paste
        selectionManager.setSelection(selectionManager.getSelectionStart(), selectionManager.getSelectionEnd());
    }
}
