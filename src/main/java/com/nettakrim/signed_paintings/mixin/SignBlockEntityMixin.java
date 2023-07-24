package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.*;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityAccessor {
    @Shadow
    private SignText frontText;
    @Shadow
    private SignText backText;

    @Unique
    protected SignSideInfo frontInfo;

    @Unique
    protected SignSideInfo backInfo;

    public PaintingInfo signedPaintings$getFrontPaintingInfo() {
        return frontInfo.paintingInfo;
    }

    public PaintingInfo signedPaintings$getBackPaintingInfo() {
        return backInfo.paintingInfo;
    }

    public Identifier signedPaintings$createBackIdentifier() {
        if (getCachedState().getBlock() instanceof AbstractSignBlock signBlock) {
            return new Identifier("block/" + signBlock.getWoodType().name() + "_planks");
        }
        return new Identifier("block/" + WoodType.OAK.name() + "_planks");
    }

    public void signedPaintings$updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        (front ? frontInfo : backInfo).updatePaintingCentering(xCentering, yCentering);
    }

    public void signedPaintings$updatePaintingSize(boolean front, float xSize, float ySize) {
        (front ? frontInfo : backInfo).updatePaintingSize(xSize, ySize);
    }

    public boolean signedPaintings$hasSignSideInfo(SignSideInfo info) {
        return frontInfo == info || backInfo == info;
    }

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    private void onInit(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        frontInfo = new SignSideInfo(frontText, null);
        backInfo = new SignSideInfo(backText, null);
    }

    @Inject(at = @At("TAIL"), method = "setText")
    private void onSetText(SignText text, boolean front, CallbackInfoReturnable<Boolean> cir) {
        frontInfo.text = frontText;
        backInfo.text = backText;
        SignSideInfo info = (front ? frontInfo : backInfo);
        boolean valid = info.paintingInfo != null;
        if (valid) {
            valid = info.updateText();
        }

        if (!valid) {
            if (SignedPaintingsClient.currentSignEdit != null) {
                SignedPaintingsClient.currentSignEdit.screen.signedPaintings$setVisibility(false);
            }
            info.loadPainting(signedPaintings$createBackIdentifier(), front, SignType.getType(getCachedState().getBlock()));
        }
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        frontInfo.text = frontText;
        backInfo.text = backText;
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString()+" at "+getPos());
        Identifier back = signedPaintings$createBackIdentifier();
        SignType.Type signType = SignType.getType(getCachedState().getBlock());
        frontInfo.loadPainting(back, true, signType);
        backInfo.loadPainting(back, false, signType);
    }
}
