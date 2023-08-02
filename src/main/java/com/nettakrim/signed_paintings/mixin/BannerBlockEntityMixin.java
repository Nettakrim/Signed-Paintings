package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntity.class)
public class BannerBlockEntityMixin implements OverlayInfoAccessor {
    @Shadow
    private Text customName;

    @Unique
    private OverlayInfo overlayInfo;

    @Inject(at = @At("TAIL"), method = "<init>*")
    private void init(CallbackInfo ci) {
        overlayInfo = new OverlayInfo();
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        if (customName != null) overlayInfo.loadOverlay(customName.getString());
    }

    @Override
    public OverlayInfo signedPaintings$getOverlayInfo() {
        return overlayInfo;
    }

    @Override
    public void signedPaintings$reloadIfNeeded() {
        if (overlayInfo != null && overlayInfo.needsReload()) {
            overlayInfo = new OverlayInfo();
            overlayInfo.loadOverlay(customName.getString());
        }
    }
}
