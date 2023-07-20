package com.nettakrim.mixin;

import com.nettakrim.PaintingInfo;
import com.nettakrim.access.AbstractSignBlockAccessor;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.WoodType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractSignBlock.class)
public abstract class AbstractSignBlockMixin extends BlockWithEntity implements AbstractSignBlockAccessor {
    protected PaintingInfo paintingInfo;

    @Final
    WoodType type;

    protected AbstractSignBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public PaintingInfo getPaintingInfo() {
        return paintingInfo;
    }

    @Override
    public void setPaintingInfo(PaintingInfo info) {
        paintingInfo = info;
    }

    @Override
    public Identifier createBackIdentifier() {
        return new Identifier("block/" + type.name() + "_planks");
    }
}
