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
public class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityAccessor {
    @Shadow
    private SignText frontText;
    @Shadow
    private SignText backText;

    @Unique
    protected PaintingInfo frontPaintingInfo;

    @Unique
    protected PaintingInfo backPaintingInfo;

    public PaintingInfo getFrontPaintingInfo() {
        return frontPaintingInfo;
    }

    public PaintingInfo getBackPaintingInfo() {
        return backPaintingInfo;
    }

    public Identifier createBackIdentifier() {
        if (getCachedState().getBlock() instanceof AbstractSignBlock signBlock) {
            return new Identifier("block/" + signBlock.getWoodType().name() + "_planks");
        }
        return new Identifier("block/" + WoodType.OAK.name() + "_planks");
    }

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString());
        loadURL(SignedPaintingsClient.combineSignText(frontText), true);
        loadURL(SignedPaintingsClient.combineSignText(backText), false);
    }

    @Unique
    private void loadURL(String url, boolean front) {
        url = SignedPaintingsClient.imageManager.tryApplyURLAliases(url);

        if (!url.contains("://")) {
            url = "https://"+url;
        }

        SignedPaintingsClient.LOGGER.info("trying to load url "+url+" at "+getPos());
        PaintingInfo info = (front ? frontPaintingInfo : backPaintingInfo);
        if (info != null) info.invalidateImage();
        ImageDataLoadInterface frontImageLoad = (data) -> frontPaintingInfo = updateInfo(frontPaintingInfo, data, true );
        ImageDataLoadInterface backImageLoad =  (data) -> backPaintingInfo  = updateInfo(backPaintingInfo,  data, false);
        SignedPaintingsClient.imageManager.loadImage(url, front ? frontImageLoad : backImageLoad);
    }

    @Unique
    private PaintingInfo updateInfo(PaintingInfo reference, ImageData data, boolean isFront) {
        SignedPaintingsClient.LOGGER.info("creating painting info for "+data+" at "+getPos());
        if (reference == null) {
            return new PaintingInfo(data, createBackIdentifier(), isFront, !(getCachedState().getBlock() instanceof SignBlock));
        } else {
            reference.updateImage(data);
            return reference;
        }
    }
}
