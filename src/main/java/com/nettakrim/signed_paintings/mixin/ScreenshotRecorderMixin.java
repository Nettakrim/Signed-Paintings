package com.nettakrim.signed_paintings.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.commands.SignedPaintingsCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;

@Mixin(Screenshot.class)
public class ScreenshotRecorderMixin {
    //@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;writeToFile(Ljava/io/File;)V"), method = "method_22691")
    //private static void onScreenshot(NativeImage nativeImage, File file, Consumer<?> consumer, CallbackInfo ci) {
    //    String filename = file.getPath().replace(SignedPaintingsClient.getScreenshotDirectory(), "");
    //    SignedPaintingsCommands.recentScreenshots.add(filename);
    //}
}
