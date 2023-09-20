package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.commands.SignedPaintingsCommands;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/NativeImage;writeTo(Ljava/io/File;)V"), method = "method_1661")
    private static void onScreenshot(NativeImage nativeImage, File file, Consumer consumer, CallbackInfo ci) {
        String filename = file.getPath().replace(SignedPaintingsClient.getScreenshotDirectory(), "");
        SignedPaintingsCommands.recentScreenshots.add(filename);
    }
}
