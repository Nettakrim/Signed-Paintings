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

    @Shadow public abstract boolean setText(SignText text, boolean front);

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
        updatePaintingCentering(front ? frontData : backData, xCentering, yCentering);
    }

    @Unique private void updatePaintingCentering(SignSideData data, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        data.paintingInfo.updateCuboidCentering(xCentering, yCentering);
        String centering = Cuboid.getNameFromCentering(true, xCentering)+Cuboid.getNameFromCentering(false, yCentering);

        //for some reason combineSignText(data.text) does not work?
        String text = SignedPaintingsClient.currentSignEdit.screen.getCombinedMessage();
        SignedPaintingsClient.currentSignEdit.screen.clear();

        int splitStart = text.indexOf(' ');
        int splitEnd;

        if (splitStart == -1) {
            splitStart = text.length();
            splitEnd = splitStart;
            centering = " "+centering;
        } else {
            splitStart++;
            splitEnd = splitStart+2;
        }

        text = text.substring(0, splitStart)+centering+text.substring(splitEnd);

        int newSelection = SignedPaintingsClient.currentSignEdit.screen.paste(text, 0, 0);
        SignedPaintingsClient.currentSignEdit.selectionManager.setSelection(newSelection, newSelection);
    }

    public void updatePaintingSize(boolean front, float xSize, float ySize) {
        (front ? frontData : backData).paintingInfo.updateCuboidSize(xSize, ySize);
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
        SignedPaintingsClient.imageManager.loadImage(url, (data) -> sideData.paintingInfo = updateInfo(sideData.paintingInfo, data, afterURL, front));

    }

    @Unique
    private PaintingInfo updateInfo(PaintingInfo reference, ImageData data, String afterURL, boolean isFront) {
        SignedPaintingsClient.LOGGER.info("creating painting info for "+data+" at "+getPos());
        PaintingInfo paintingInfo;

        if (reference == null) {
            paintingInfo = new PaintingInfo(data, createBackIdentifier(), isFront, !(getCachedState().getBlock() instanceof SignBlock));
        } else {
            reference.updateImage(data);
            paintingInfo = reference;
        }

        //this should probably be done through regex groups, to cleanly extract positioning, sizing data etc. regardless of order?
        SignedPaintingsClient.LOGGER.info("loading extra data \""+afterURL+"\"");
        if (afterURL.length() > 1) {
            String xCentering = afterURL.substring(0,1);
            String yCentering = afterURL.substring(1,2);
            paintingInfo.updateCuboidCentering(Cuboid.getCenteringFromName(xCentering), Cuboid.getCenteringFromName(yCentering));
        }

        return paintingInfo;
    }
}
