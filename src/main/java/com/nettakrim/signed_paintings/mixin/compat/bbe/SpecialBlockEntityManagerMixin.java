package com.nettakrim.signed_paintings.mixin.compat.bbe;

import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpecialBlockEntityManager.class)
public class SpecialBlockEntityManagerMixin {
    @Unique
    private static boolean vanillaIsVisible(final BlockEntity blockEntity, final Vec3 cameraPosition) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPosition, 64);
    }
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private static void forceRenderSignedPaintings(BlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (blockEntity instanceof SignBlockEntity sign) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera == null) return;

            var signAccessor = (SignBlockEntityAccessor) sign;
            if (SignedPaintingsClient.paintingRenderer.renderWithReducedCulling(signAccessor)
                || (vanillaIsVisible(blockEntity, camera.position()) && signAccessor.signedPaintings$shouldHide())) {
                cir.setReturnValue(true);
            }
        }
    }
}