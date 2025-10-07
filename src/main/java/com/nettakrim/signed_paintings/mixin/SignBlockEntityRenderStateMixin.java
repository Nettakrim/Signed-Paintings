package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignBlockEntityRenderState.class)
public class SignBlockEntityRenderStateMixin implements SignBlockEntityRenderStateAccessor {
    @Unique
    PaintingInfo frontInfo;

    @Unique
    PaintingInfo backInfo;

    @Override
    public void signedPaintings$setFrontInfo(PaintingInfo info) {
        frontInfo = info;
    }

    @Override
    public void signedPaintings$setBackInfo(PaintingInfo info) {
        backInfo = info;
    }

    @Override
    public PaintingInfo signedPaintings$getFrontInfo() {
        return frontInfo;
    }

    @Override
    public PaintingInfo signedPaintings$getBackInfo() {
        return backInfo;
    }
}
