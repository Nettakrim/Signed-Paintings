package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignRenderState.class)
public class SignBlockEntityRenderStateMixin implements SignBlockEntityRenderStateAccessor {
    @Unique
    PaintingInfo frontInfo;

    @Unique
    PaintingInfo backInfo;

    @Unique
    float rotation;

    @Override
    public void signedPaintings$setFrontInfo(PaintingInfo info) {
        frontInfo = info;
    }

    @Override
    public void signedPaintings$setBackInfo(PaintingInfo info) {
        backInfo = info;
    }

    @Override
    public void signedPaintings$setRotation(float rotation) {
        this.rotation = rotation;
    }

    @Override
    public PaintingInfo signedPaintings$getFrontInfo() {
        return frontInfo;
    }

    @Override
    public PaintingInfo signedPaintings$getBackInfo() {
        return backInfo;
    }

    @Override
    public float signedPaintings$getRotation() {
        return rotation;
    }
}
