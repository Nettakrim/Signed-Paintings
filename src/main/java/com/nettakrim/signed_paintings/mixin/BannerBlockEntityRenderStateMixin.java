package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.BannerBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BannerRenderState.class)
public class BannerBlockEntityRenderStateMixin implements BannerBlockEntityRenderStateAccessor {
    @Unique
    private OverlayInfo overlayInfo;

    @Override
    public void signedPaintings$setOverlayInfo(OverlayInfo info) {
        this.overlayInfo = info;
    }

    @Override
    public OverlayInfo signedPaintings$getOverlayInfo() {
        return overlayInfo;
    }
}
