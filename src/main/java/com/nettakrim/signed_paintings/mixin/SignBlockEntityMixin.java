package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.Cuboid;
import com.nettakrim.signed_paintings.PaintingInfo;
import com.nettakrim.signed_paintings.SignSideInfo;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
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
        (front ? frontInfo : backInfo).updateText();
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        frontInfo.text = frontText;
        backInfo.text = backText;
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString()+" at "+getPos());
        Identifier back = signedPaintings$createBackIdentifier();
        boolean isWall = !(getCachedState().getBlock() instanceof SignBlock);
        frontInfo.loadPainting(back, true, isWall);
        backInfo.loadPainting(back, false, isWall);
    }
}
