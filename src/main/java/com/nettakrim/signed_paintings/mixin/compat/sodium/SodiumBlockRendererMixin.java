package com.nettakrim.signed_paintings.mixin.compat.sodium;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(BlockRenderer.class)
public class SodiumBlockRendererMixin {
    @Inject(
            method = "renderModel",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void hideSignModel(
            BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin,
            CallbackInfo ci
    ) {
        if (!SignedPaintingsClient.renderSigns) return;

        if (state.hasBlockEntity()) {
            LevelSlice slice = ((AbstractBlockRenderContextAccessor) (Object) this).getSlice();
            BlockEntity blockEntity = slice.getBlockEntity(pos);
            if (blockEntity instanceof SignBlockEntityAccessor signBlock) {
                if (signBlock.signedPaintings$shouldHide()) {
                    ci.cancel();
                    return;
                } else {
                    signBlock.signedPaintings$tryRegisterRefreshOnLoadCallback();
                }
            }
        }
    }
}