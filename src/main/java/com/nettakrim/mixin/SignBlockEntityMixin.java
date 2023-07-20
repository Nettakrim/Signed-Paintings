package com.nettakrim.mixin;

import com.nettakrim.Cuboid;
import com.nettakrim.PaintingInfo;
import com.nettakrim.SignedPaintingsClient;
import com.nettakrim.access.AbstractSignBlockAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin extends BlockEntity {
    @Shadow
    private SignText frontText;
    @Shadow
    private SignText backText;

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}

    @Inject(at = @At("HEAD"), method = "setText")
    private void setText(SignText text, boolean front, CallbackInfoReturnable<Boolean> cir) {
        SignedPaintingsClient.LOGGER.info("set text "+frontText.getMessage(0, false).toString());
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    private void init(BlockEntityType blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        SignedPaintingsClient.LOGGER.info("new Sign "+frontText.getMessage(0, false).toString());
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void onNBTRead(NbtCompound nbt, CallbackInfo ci) {
        SignedPaintingsClient.LOGGER.info("nbt read "+frontText.getMessage(0, false).toString());
        loadURL(combineSignText(frontText));
    }

    private void loadURL(String url) {
        if (!url.startsWith("http")) {
            url = "https://"+url;
        }
        SignedPaintingsClient.LOGGER.info("trying to load url "+url+" at "+getPos());
        SignedPaintingsClient.imageManager.registerImage(url);
        SignedPaintingsClient.LOGGER.info("cheching accessor for url "+url+" at "+getPos());
        if (getCachedState().getBlock() instanceof AbstractSignBlockAccessor accessor) {
            SignedPaintingsClient.LOGGER.info("creating painting info for "+url+" at "+getPos());
            PaintingInfo info = accessor.getPaintingInfo();
            if (info == null) {
                accessor.setPaintingInfo(new PaintingInfo(new Cuboid(2, 3, 0.0625f, 0.5f, 1, 0.5f + (0.0625f / 2)), url, accessor.createBackIdentifier()));
            } else {
                info.updateImage(url);
            }
        }
    }

    private String combineSignText(SignText text) {
        Text[] layers = text.getMessages(false);
        String combined = layers[0].getString();
        combined += layers[1].getString();
        combined += layers[2].getString();
        combined += layers[3].getString();
        return combined;
    }
}
