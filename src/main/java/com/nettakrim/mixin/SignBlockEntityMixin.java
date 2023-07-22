package com.nettakrim.mixin;

import com.nettakrim.*;
import com.nettakrim.access.SignBlockEntityAccessor;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityAccessor {
    @Shadow
    private SignText frontText;
    @Shadow
    private SignText backText;

    @Unique
    protected SignSideData frontData;

    @Unique
    protected SignSideData backData;

    public PaintingInfo getFrontPaintingInfo() {
        return frontData.paintingInfo;
    }

    public PaintingInfo getBackPaintingInfo() {
        return backData.paintingInfo;
    }

    public Identifier createBackIdentifier() {
        if (getCachedState().getBlock() instanceof AbstractSignBlock signBlock) {
            return new Identifier("block/" + signBlock.getWoodType().name() + "_planks");
        }
        return new Identifier("block/" + WoodType.OAK.name() + "_planks");
    }

    public void updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        (front ? frontData : backData).updatePaintingCentering(xCentering, yCentering);
    }

    public void updatePaintingSize(boolean front, float xSize, float ySize) {
        (front ? frontData : backData).updatePaintingSize(xSize, ySize);
    }

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    private void onInit(BlockEntityType blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        frontData = new SignSideData(frontText, null);
        backData = new SignSideData(backText, null);
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString());
        loadPainting(SignedPaintingsClient.combineSignText(frontText), true);
        loadPainting(SignedPaintingsClient.combineSignText(backText), false);
    }

    @Unique
    private void loadPainting(String text, boolean front) {
        String[] parts = text.split(" ", 2);
        loadURL(parts[0], parts.length > 1 ? parts[1] : "", front);
    }

    @Unique
    private void loadURL(String url, String afterURL, boolean front) {
        url = SignedPaintingsClient.imageManager.tryApplyURLAliases(url);

        if (!url.contains("://")) {
            url = "https://"+url;
        }

        SignedPaintingsClient.LOGGER.info("trying to load url "+url+" at "+getPos());
        SignSideData sideData = front ? frontData : backData;
        if (sideData.paintingInfo != null) sideData.paintingInfo.invalidateImage();
        SignedPaintingsClient.imageManager.loadImage(url, (data) -> sideData.updateInfo(data, afterURL, createBackIdentifier(), front, !(getCachedState().getBlock() instanceof SignBlock)));
    }
}
