package com.nettakrim.signed_paintings.mixin;

import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SignEditScreen.class)
public class SignEditScreenMixin {
    /*
    @WrapOperation(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"))
    private void offsetSign(GuiGraphicsExtractor instance, Model.Simple model, float scale, WoodType woodType, int x1, int y1, int x2, int y2, Operation<Void> original) {
        int state = ((AbstractSignEditScreenAccessor)this).signedPaintings$internalRenderState();
        if (state == 0) {
            original.call(instance, model, scale, woodType, x1, y1, x2, y2);
            return;
        }
        int x = 38;
        int y = -25 + state;
        original.call(instance, model, scale * 0.5f, woodType, x, y, (x2 - x1) + x, (y2 - y1) + y);
    }

     */
}
