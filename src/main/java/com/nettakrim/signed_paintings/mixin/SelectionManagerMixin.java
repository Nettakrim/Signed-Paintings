package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;
import net.minecraft.client.gui.font.TextFieldHelper;

@Mixin(TextFieldHelper.class)
public class SelectionManagerMixin {
    @Final
    @Shadow private Supplier<String> getClipboardFn;

    @Shadow private int cursorPos;
    @Shadow private int selectionPos;

    @Inject(at = @At("HEAD"), method = "paste", cancellable = true)
    private void onPaste(CallbackInfo ci) {
        if (SignedPaintingsClient.currentSignEdit == null) return;
        cursorPos = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$paste(this.getClipboardFn.get(), cursorPos, selectionPos, true);
        selectionPos = cursorPos;
        ci.cancel();
    }
}
