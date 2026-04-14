package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class PaintingInfo extends ImageInfo {
    public final BlockEntity blockEntity;
    public final boolean isFront;
    public final SignType.Type signType;

    private TextureAtlasSprite back;

    public Vector3f rotationVec = zero;
    public Vector3f offsetVec = zero;

    private float width;
    private float height;
    private float depth;

    private Centering.Type xCentering;
    private Centering.Type yCentering;

    private BackType.Type backType = BackType.Type.SIGN;

    private float pixelsPerBlock;

    private static final Vector3f zero = new Vector3f(0,0,0);

    public boolean working;
    private boolean needsBackUpdate = false;

    public PaintingInfo(ImageData image, boolean isFront, SignBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.image = image;
        this.signType = SignType.getType(blockEntity.getBlockState().getBlock());
        this.isFront = isFront;
        resetCuboid();
    }

    private void resetCuboid() {
        this.depth = 1 / 16f;
        this.xCentering = Centering.Type.CENTER;
        this.yCentering = signType == SignType.Type.STANDING ? Centering.Type.MIN : Centering.Type.CENTER;
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

    public int getCenterIndex() {
        return (2-xCentering.getIndex()) + yCentering.getIndex()*3;
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
            Level world = this.blockEntity.getLevel();
            if (world == null) world = SignedPaintingsClient.client.level;
            if (world == null) {
                needsBackUpdate = true;
                return;
            }
            BlockPos blockPos = this.blockEntity.getBlockPos();
            double rotation = ((SignBlock)this.blockEntity.getBlockState().getBlock()).getYRotationDegrees(this.blockEntity.getBlockState());
            blockPos = switch (signType) {
                case STANDING -> blockPos.below();
                case WALL -> blockPos.relative(Direction.fromYRot(rotation+180), 1);
                case HANGING -> blockPos.above();
                case WALL_HANGING -> getSolidWallHang(world, blockPos, Direction.fromYRot(rotation+90));
            };
            blockState = world.getBlockState(blockPos);

            // while the world is loading it can end up detecting void air instead of the actual block
            // the performance impact if the sign actually does have void air should be negligible, as well as unlikely
            if (blockState.is(Blocks.VOID_AIR)) {
                needsBackUpdate = true;
            }
        }

        if (blockState == null || blockState.isAir()) blockState = this.blockEntity.getBlockState();

        TextureAtlasSprite back = null;
        if (this.backType == BackType.Type.SIGN) {
            String name = ((SignBlock) this.blockEntity.getBlockState().getBlock()).type().name();
            try {
                back = SignedPaintingsClient.client.getAtlasManager().get(new SpriteId( Identifier.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png"), Identifier.fromNamespaceAndPath("minecraft", "block/" + name + "_planks")));
            } catch (Exception ignored) {}
        }
        if (back == null) {
            back = SignedPaintingsClient.client.getModelManager().getBlockStateModelSet().getParticleMaterial(blockState).sprite();
        }
        this.back = back;
    }

    private BlockPos getSolidWallHang(Level world, BlockPos blockPos, Direction direction) {
        return blockPos.relative(direction, world.getBlockState(blockPos.relative(direction, 1)).isAir() ? -1 : 1);
    }

    @Override
    public Identifier getImageIdentifier() {
        if (pixelsPerBlock == 0) {
            return super.getImageIdentifier();
        } else {
            return image.getIdentifier(Math.round(width*pixelsPerBlock), Math.round(height*pixelsPerBlock), working);
        }
    }

    public TextureAtlasSprite getBackSprite() {
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
