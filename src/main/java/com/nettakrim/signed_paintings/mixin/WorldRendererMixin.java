package com.nettakrim.signed_paintings.mixin;

/*
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @SuppressWarnings("rawtypes")
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V", ordinal = 0, shift = At.Shift.AFTER), method = "method_62214")
    void renderTransparentQueue(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState, Profiler profiler, Matrix4f matrix4f, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        SignedPaintingsClient.paintingRenderer.renderTranslucentQueue(bufferBuilders.getEntityVertexConsumers());
    }
}
*/