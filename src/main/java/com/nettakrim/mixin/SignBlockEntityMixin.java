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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    public PaintingInfo signedPaintings$getFrontPaintingInfo() {
        return frontData.paintingInfo;
    }

    public PaintingInfo signedPaintings$getBackPaintingInfo() {
        return backData.paintingInfo;
    }

    public Identifier signedPaintings$createBackIdentifier() {
        if (getCachedState().getBlock() instanceof AbstractSignBlock signBlock) {
            return new Identifier("block/" + signBlock.getWoodType().name() + "_planks");
        }
        return new Identifier("block/" + WoodType.OAK.name() + "_planks");
    }

    public void signedPaintings$updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        (front ? frontData : backData).updatePaintingCentering(xCentering, yCentering);
    }

    public void signedPaintings$updatePaintingSize(boolean front, float xSize, float ySize) {
        (front ? frontData : backData).updatePaintingSize(xSize, ySize);
    }

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    private void onInit(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        frontData = new SignSideData(frontText, null);
        backData = new SignSideData(backText, null);
    }

    @Inject(at = @At("TAIL"), method = "setText")
    private void onSetText(SignText text, boolean front, CallbackInfoReturnable<Boolean> cir) {
        frontData.text = frontText;
        backData.text = backText;
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        frontData.text = frontText;
        backData.text = backText;
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString()+" at "+getPos());
        Identifier back = signedPaintings$createBackIdentifier();
        boolean isWall = !(getCachedState().getBlock() instanceof SignBlock);
        frontData.loadPainting(back, true, isWall);
        backData.loadPainting(back, false, isWall);
    }
}
