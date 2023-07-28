package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageData;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PaintingInfo {
    public BlockEntity blockEntity;
    public Cuboid cuboid;
    private ImageData image;
    private Sprite back;
    public SignType.Type signType;
    public float rotation;
    private float width;
    private float height;
    private float depth;
    private float xOffset;
    private float yOffset;
    private float zOffset;
    private Centering.Type xCentering;
    private Centering.Type yCentering;
    private BackType.Type backType;
    private float pixelsPerBlock;
    public boolean working;

    public PaintingInfo(ImageData image, boolean isFront, SignBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.image = image;
        this.signType = SignType.getType(blockEntity.getCachedState().getBlock());
        this.rotation = isFront ? 0 : 180;
        resetCuboid();
    }

    public void updateImage(ImageData image) {
        this.image = image;
        resetCuboid();
    }

    public void invalidateImage() {
        this.image = null;
    }

    public boolean isReady() {
        return image != null && image.ready;
    }

    private void resetCuboid() {
        this.depth = 1 / 16f;
        this.xCentering = Centering.Type.CENTER;
        this.yCentering = Centering.Type.CENTER;
        resetSize();
    }

    public void resetSize() {
        this.width = image.width/16f;
        this.height = image.height/16f;
        while (this.width > 8 || this.height > 8) {
            this.width /= 2f;
            this.height /= 2f;
        }
        this.width = SignedPaintingsClient.roundFloatTo3DP(this.width);
        this.height = SignedPaintingsClient.roundFloatTo3DP(this.height);
        updateCuboid();
    }

    private void updateCuboid() {
        float reducedDepth = this.depth;
        if (backType == BackType.Type.NONE) reducedDepth = 1/256f;
        this.cuboid = switch (signType) {
            case WALL                  -> Cuboid.CreateWallCuboid(    width, xCentering, height, yCentering, reducedDepth, xOffset, yOffset, zOffset);
            case STANDING              -> Cuboid.CreateFlushCuboid(   width, xCentering, height, yCentering, reducedDepth, xOffset, yOffset, zOffset);
            case HANGING, WALL_HANGING -> Cuboid.CreateCentralCuboid( width, xCentering, height, yCentering, reducedDepth, xOffset, yOffset, zOffset);
        };
    }

    public void updateCuboidCentering(Centering.Type xCentering, Centering.Type yCentering) {
        this.xCentering = xCentering;
        this.yCentering = yCentering;
        updateCuboid();
    }

    public void updateCuboidSize(float xSize, float ySize) {
        this.width = xSize;
        this.height = ySize;
        updateCuboid();
    }

    public void updateCuboidOffset(float xOffset, float yOffset, float zOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        updateCuboid();
    }

    public float getYOffset() {
        return yOffset;
    }

    public void updatePixelsPerBlock(float pixelsPerBlock) {
        this.pixelsPerBlock = pixelsPerBlock;
    }

    public float getPixelsPerBlock() {
        return pixelsPerBlock;
    }

    public void setBackType(BackType.Type backType) {
        this.backType = backType;
        updateBack();
        updateCuboid();
    }

    public BackType.Type getBackType() {
        return backType;
    }

    private void updateBack() {
        BlockState blockState = null;
        if (this.backType == BackType.Type.BLOCK) {
            World world = this.blockEntity.getWorld();
            if (world == null) world = SignedPaintingsClient.client.world;
            BlockPos blockPos = this.blockEntity.getPos();
            double rotation = ((AbstractSignBlock)this.blockEntity.getCachedState().getBlock()).getRotationDegrees(this.blockEntity.getCachedState());
            blockPos = switch (signType) {
                case STANDING -> blockPos.down();
                case WALL -> blockPos.offset(Direction.fromRotation(rotation+180), 1);
                case HANGING -> blockPos.up();
                case WALL_HANGING -> getSolidWallHang(world, blockPos, Direction.fromRotation(rotation+90));
            };
            blockState = world.getBlockState(blockPos);
        }

        if (blockState == null || blockState.isAir()) blockState = this.blockEntity.getCachedState();

        ModelIdentifier modelIdentifier = BlockModels.getModelId(blockState);
        this.back = SignedPaintingsClient.client.getBakedModelManager().getModel(modelIdentifier).getParticleSprite();
    }

    private BlockPos getSolidWallHang(World world, BlockPos blockPos, Direction direction) {
        return blockPos.offset(direction, world.getBlockState(blockPos.offset(direction, 1)).isAir() ? -1 : 1);
    }

    public Identifier getImageIdentifier() {
        if (pixelsPerBlock == 0) {
            return image.getBaseIdentifier();
        } else {
            return image.getIdentifier(Math.round(width*pixelsPerBlock), Math.round(height*pixelsPerBlock), working);
        }
    }

    public Sprite getBackSprite() {
        return back;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
