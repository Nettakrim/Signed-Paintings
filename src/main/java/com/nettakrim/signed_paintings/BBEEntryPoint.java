package com.nettakrim.signed_paintings;

import betterblockentities.data.SupportedBlockEntityTypes;
import betterblockentities.registration.AltRendererRegistration;
import betterblockentities.registration.BBEApiEntryPoint;
import betterblockentities.render.AltRenderer;
import betterblockentities.render.AltRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.access.BannerBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BBEEntryPoint implements BBEApiEntryPoint {
    @Override
    public void registerRenderers(AltRendererRegistration context) {
        context.registerRenderer(SupportedBlockEntityTypes.SIGN, BBESignRenderer::new);
        context.registerRenderer(SupportedBlockEntityTypes.HANGING_SIGN, BBESignRenderer::new);
        context.registerRenderer(SupportedBlockEntityTypes.BANNER, BBEBannerRenderer::new);
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

    private static class BBEBannerRenderer implements AltRenderer<BannerBlockEntity, BannerRenderState> {
        public BBEBannerRenderer(final AltRendererProvider.Context context) {

        }

        public BannerRenderState createRenderState() {
            return new BannerRenderState();
        }

        public void extractRenderState(final BannerBlockEntity blockEntity, final BannerRenderState renderState, final float partialTicks, final Vec3 cameraPosition, final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
            AltRenderer.super.extractRenderState(blockEntity, renderState, partialTicks, cameraPosition, breakProgress);

            BlockState blockState = blockEntity.getBlockState();
            if (blockState.getBlock() instanceof BannerBlock) {
                renderState.transformation = BannerRenderer.TRANSFORMATIONS.freeTransformations(blockState.getValue(BannerBlock.ROTATION));
                renderState.attachmentType = BannerBlock.AttachmentType.GROUND;
            } else {
                renderState.transformation = BannerRenderer.TRANSFORMATIONS.wallTransformation(blockState.getValue(WallBannerBlock.FACING));
                renderState.attachmentType = BannerBlock.AttachmentType.WALL;
            }

            OverlayInfoAccessor accessor = (OverlayInfoAccessor) blockEntity;
            accessor.signedPaintings$reloadIfNeeded();
            ((BannerBlockEntityRenderStateAccessor)renderState).signedPaintings$setOverlayInfo(accessor.signedPaintings$getOverlayInfo());
        }

        public void submit(final BannerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
            if (!SignedPaintingsClient.renderBanners) {
                return;
            }

            OverlayInfo overlayInfo = ((BannerBlockEntityRenderStateAccessor)state).signedPaintings$getOverlayInfo();
            if (overlayInfo == null || !overlayInfo.isReady()) return;

            poseStack.pushPose();
            poseStack.mulPose(state.transformation);
            boolean standing = state.attachmentType == BannerBlock.AttachmentType.GROUND;
            poseStack.translate(0.0f, (standing ? -44.0f : -20.5f) / 16f, (standing ? 0.0f : 10.5f) / 16f);
            poseStack.scale(1.5f, -1.5f, 1f);
            poseStack.translate(0, 0, -0.2f);
            SignedPaintingsClient.paintingRenderer.renderOverlay(poseStack, submitNodeCollector, overlayInfo, state.lightCoords);
            poseStack.popPose();
        }
    }
}
