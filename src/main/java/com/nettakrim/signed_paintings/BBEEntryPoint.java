package com.nettakrim.signed_paintings;

import betterblockentities.data.SupportedBlockEntityTypes;
import betterblockentities.registration.AltRendererRegistration;
import betterblockentities.registration.BBEApiEntryPoint;
import betterblockentities.render.AltRenderer;
import betterblockentities.render.AltRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

public class BBEEntryPoint implements BBEApiEntryPoint {
    @Override
    public void registerRenderers(AltRendererRegistration context) {
        context.registerRenderer(SupportedBlockEntityTypes.SIGN, BBESignRenderer::new);
        context.registerRenderer(SupportedBlockEntityTypes.HANGING_SIGN, BBESignRenderer::new);
    }

    private static class BBESignRenderer implements AltRenderer<SignBlockEntity, SignRenderState> {
        public BBESignRenderer(final AltRendererProvider.Context context) {

        }

        public SignRenderState createRenderState() {
            return new SignRenderState();
        }

        public void extractRenderState(final SignBlockEntity blockEntity, final SignRenderState renderState, final float partialTicks, final Vec3 cameraPosition, final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
            AltRenderer.super.extractRenderState(blockEntity, renderState, partialTicks, cameraPosition, breakProgress);
            SignedPaintingsClient.paintingRenderer.modifySignRenderState(blockEntity, renderState);
        }

        public void submit(final SignRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
            SignedPaintingsClient.paintingRenderer.renderSignPaintings(state, poseStack, submitNodeCollector);
        }

        @Override
        public boolean shouldRender(SignBlockEntity blockEntity, Vec3 cameraPosition) {
            return SignedPaintingsClient.paintingRenderer.renderWithReducedCulling((SignBlockEntityAccessor)blockEntity) || AltRenderer.super.shouldRender(blockEntity, cameraPosition);
        }
    }
}
