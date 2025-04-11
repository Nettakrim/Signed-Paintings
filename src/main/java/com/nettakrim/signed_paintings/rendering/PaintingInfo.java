package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageData;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class PaintingInfo {
    public final BlockEntity blockEntity;
    public final boolean isFront;
    public final SignType.Type signType;

    private ImageData image;
    public Cuboid cuboid;
    private Sprite back;

    public Vector3f rotationVec = zero;
    public Vector3f offsetVec = zero;

    private float width;
    private float height;
    private float depth;

    private Centering.Type xCentering = Centering.Type.CENTER;
    private Centering.Type yCentering = Centering.Type.CENTER;

    private BackType.Type backType = BackType.Type.SIGN;

    private float pixelsPerBlock;

    private static final Vector3f zero = new Vector3f(0,0,0);

    public boolean working;
    private boolean needsBackUpdate = false;

    public PaintingInfo(ImageData image, boolean isFront, SignBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.image = image;
        this.signType = SignType.getType(blockEntity.getCachedState().getBlock());
        this.isFront = isFront;
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

    public boolean needsReload() {
        return image != null && image.needsReload;
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
            case WALL                  -> Cuboid.CreateWallCuboid(    width, xCentering, height, yCentering, reducedDepth);
            case STANDING              -> Cuboid.CreateFlushCuboid(   width, xCentering, height, yCentering, reducedDepth);
            case HANGING, WALL_HANGING -> Cuboid.CreateCentralCuboid( width, xCentering, height, yCentering, reducedDepth);
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

    public void updateOffsetVec(Vector3f offsetVec) {
        this.offsetVec = offsetVec;
        updateCuboid();
    }

    public void updateRotationVec(Vector3f rotationVec) {
        this.rotationVec = rotationVec;
        updateCuboid();
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
            if (world == null) {
                needsBackUpdate = true;
                return;
            }
            BlockPos blockPos = this.blockEntity.getPos();
            double rotation = ((AbstractSignBlock)this.blockEntity.getCachedState().getBlock()).getRotationDegrees(this.blockEntity.getCachedState());
            blockPos = switch (signType) {
                case STANDING -> blockPos.down();
                case WALL -> blockPos.offset(Direction.fromHorizontalDegrees(rotation+180), 1);
                case HANGING -> blockPos.up();
                case WALL_HANGING -> getSolidWallHang(world, blockPos, Direction.fromHorizontalDegrees(rotation+90));
            };
            blockState = world.getBlockState(blockPos);

            // while the world is loading it can end up detecting void air instead of the actual block
            // the performance impact if the sign actually does have void air should be negligible, as well as unlikely
            if (blockState.isOf(Blocks.VOID_AIR)) {
                needsBackUpdate = true;
            }
        }

        if (blockState == null || blockState.isAir()) blockState = this.blockEntity.getCachedState();

        Sprite back = null;
        if (this.backType == BackType.Type.SIGN) {
            String name = ((AbstractSignBlock) this.blockEntity.getCachedState().getBlock()).getWoodType().name();
            try {
                back = SignedPaintingsClient.client.getSpriteAtlas(Identifier.of("minecraft", "textures/atlas/blocks.png")).apply(Identifier.of("minecraft", "block/" + name + "_planks"));
            } catch (Exception ignored) {}
        }
        if (back == null) back = SignedPaintingsClient.client.getBakedModelManager().getBlockModels().getModelParticleSprite(blockState);
        if (back == null) back = SignedPaintingsClient.client.getBakedModelManager().getMissingModel().particleSprite();
        this.back = back;
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
        if (needsBackUpdate || back == null) updateBack();
        return back;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
