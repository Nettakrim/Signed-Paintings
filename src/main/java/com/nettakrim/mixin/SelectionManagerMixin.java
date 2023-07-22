package com.nettakrim.mixin;

import com.nettakrim.SignedPaintingsClient;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(SelectionManager.class)
public class SelectionManagerMixin {
    @Final
    @Shadow private Supplier<String> clipboardGetter;

    @Shadow private int selectionStart;
    @Shadow private int selectionEnd;

    @Inject(at = @At("HEAD"), method = "paste", cancellable = true)
    private void onPaste(CallbackInfo ci) {
        if (SignedPaintingsClient.currentSignEdit == null) return;
        selectionStart = SignedPaintingsClient.currentSignEdit.screen.paste(this.clipboardGetter.get(), selectionStart, selectionEnd);
        selectionEnd = selectionStart;
        ci.cancel();
    }
}
