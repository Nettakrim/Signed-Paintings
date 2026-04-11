package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.util.ImageData;
import com.nettakrim.signed_paintings.util.SignByteMapper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Vector3f;

public class SignSideInfo {
    public SignText text;
    public PaintingInfo paintingInfo;

    private PaintingDataCache cache;

    private boolean updatingSignText;

    public SignSideInfo(SignText text, PaintingInfo paintingInfo) {
        this.text = text;
        this.paintingInfo = paintingInfo;
    }

    public void loadPainting(boolean isFront, SignBlockEntity blockEntity, boolean working) {
        String[] parts = getParts(working);
        cache = new PaintingDataCache(parts[0]);
        String url = SignedPaintingsClient.imageManager.applyURLInferences(parts[0]);
        loadURL(url, parts.length > 1 ? parts[1] : "", isFront, blockEntity, working);
    }

    public String getData(){
        String[] parts = getParts(true);
        if (parts.length == 1) {
            return parts[0]+"|";
        }
        return String.join("|", parts);
    }

    public String getUrl(){
        return SignedPaintingsClient.imageManager.applyURLInferences(getParts(true)[0]);
    }

    private String[] getParts(boolean working) {
        String combinedText = !working || SignedPaintingsClient.currentSignEdit == null ? SignedPaintingsClient.combineSignText(text) : SignedPaintingsClient.currentSignEdit.screen.signedPaintings$getText();
        if (combinedText.startsWith(SignByteMapper.INITIALIZER_STRING)) {
            String[] parts = combinedText.substring(2).split(SignByteMapper.DELIMITER, 2);
            if (parts.length > 0) {
                parts[0] = SignByteMapper.decode(parts[0]);
                // Converted to previous format
                combinedText = parts.length > 1 ? parts[0] + '|' + parts[1] : parts[0];
            }

            return combinedText.split("[\\n ]|(\\|)", 2);
        }

        // only allow encoded text
        return new String[]{""};
    }

    private void loadURL(String url, String afterURL, boolean isFront, SignBlockEntity blockEntity, boolean working) {
        if (paintingInfo != null) paintingInfo.invalidateImage();
        SignedPaintingsClient.imageManager.loadImage(url, (data) -> updateInfo(data, afterURL, isFront, blockEntity, working));
    }

    public void updateInfo(ImageData data, String afterURL, boolean isFront, SignBlockEntity blockEntity, boolean working) {
        SignedPaintingsClient.info("updating painting info for "+data.getBaseIdentifier(), false);
        if (paintingInfo == null) {
            paintingInfo = new PaintingInfo(data, isFront, blockEntity);
        } else {
            paintingInfo.updateImage(data);
        }
        paintingInfo.working = working;

        cache.init(paintingInfo, paintingInfo.signType);

        SignedPaintingsClient.info("loading extra data \""+afterURL+"\"", false);
        updateCache(afterURL);

        if (data.ready && SignedPaintingsClient.currentSignEdit != null && ((SignBlockEntityAccessor)SignedPaintingsClient.currentSignEdit.sign).signedPaintings$hasSignSideInfo(this)) {
            SignedPaintingsClient.currentSignEdit.screen.signedPaintings$setVisibility(true);
            SignedPaintingsClient.currentSignEdit.screen.signedPaintings$initSliders(this);
        }
    }

    public void updatePaintingCentering(Centering.Type xCentering, Centering.Type yCentering) {
        if (paintingInfo == null) return;
        paintingInfo.updateCuboidCentering(xCentering, yCentering);
        cache.xCentering = xCentering;
        cache.yCentering = yCentering;
        updateSignText();
    }

    public void updatePaintingSize(float xSize, float ySize) {
        if (paintingInfo == null) return;
        paintingInfo.updateCuboidSize(xSize, ySize);
        cache.width = xSize;
        cache.height = ySize;
        updateSignText();
    }

    public void updatePaintingOffset(Vector3f vec) {
        if (paintingInfo == null) return;
        paintingInfo.updateOffsetVec(vec);
        cache.offsetVec = vec;
        updateSignText();
    }

    public void updateRotatingVector(Vector3f vec) {
        if (paintingInfo == null) return;
        paintingInfo.updateRotationVec(vec);
        cache.rotationVec = vec;
        updateSignText();
    }

    public void updatePaintingPixelsPerBlock(float pixelsPerBlock) {
        if (paintingInfo == null) return;
        paintingInfo.updatePixelsPerBlock(pixelsPerBlock);
        cache.pixelsPerBlock = pixelsPerBlock;
        updateSignText();
    }

    public BackType.Type cyclePaintingBack() {
        cache.backType = BackType.cycle(cache.backType);
        paintingInfo.setBackType(cache.backType);
        updateSignText();
        return cache.backType;
    }

    public void resetSize() {
        paintingInfo.resetSize();
        cache.width = paintingInfo.getWidth();
        cache.height = paintingInfo.getHeight();
        updateSignText();
    }

    private void updateSignText() {
        updatingSignText = true;
        cache.updateSignText();
        updatingSignText = false;
    }

    public boolean updateText() {
        if (paintingInfo == null) return false;
        if (updatingSignText) return true;
        String[] parts = getParts(true);
        if (!cache.url.equals(parts[0])) {
            return false;
        }
        if (parts.length > 1) {
            updateCache(parts[1]);
        }
        return true;
    }

    private void updateCache(String afterUrl) {
        cache.parseAfterUrl(afterUrl);

        paintingInfo.updateCuboidCentering(cache.xCentering, cache.yCentering);
        paintingInfo.updateCuboidSize(cache.width, cache.height);
        paintingInfo.setBackType(cache.backType);
        paintingInfo.updateOffsetVec(cache.offsetVec);
        paintingInfo.updatePixelsPerBlock(cache.pixelsPerBlock);
        paintingInfo.updateRotationVec(cache.rotationVec);
    }

    public static Vector3f getRotVector(String s) {
        String[] parts = s.split(",");
        if (parts.length == 3) {
            return new Vector3f(
                    Float.parseFloat(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2])
            );
        }
        return null;
    }

    private static class PaintingDataCache {
        private final String url;
        private Centering.Type xCentering = Centering.Type.CENTER;
        private Centering.Type yCentering = Centering.Type.CENTER;
        private float width;
        private float height;
        private BackType.Type backType = BackType.Type.SIGN;

        private Vector3f offsetVec;
        private Vector3f rotationVec;
        private float pixelsPerBlock;
        private String extraText;

        public PaintingDataCache(String url) {
            this.url = url;
        }

        public void init(PaintingInfo paintingInfo, SignType.Type signType) {
            this.xCentering = Centering.Type.CENTER;
            this.yCentering = signType == SignType.Type.STANDING ? Centering.Type.MIN : Centering.Type.CENTER;
            this.width = paintingInfo.getWidth();
            this.height = paintingInfo.getHeight();
            this.backType = BackType.Type.SIGN;
            this.offsetVec = new Vector3f(0, 0, 0);
            this.rotationVec = new Vector3f(0, 0, 0);
        }

        public void parseAfterUrl(String s) {
            String[] parts = s.split("[|\n ]");

            int currentIndex = 0;

            if (currentIndex < parts.length && tryParseCharFlags(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParseSize(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParseYOffset(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParsePixelsPerBlock(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParseXOffset(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParseZOffset(parts[currentIndex])) currentIndex++;
            
            if (currentIndex < parts.length && tryParseRotationVec(parts[currentIndex])) currentIndex++;

            StringBuilder builder = new StringBuilder();
            for (int i = currentIndex; i < parts.length; i++) {
                if (!builder.isEmpty()) builder.append(" ");
                builder.append(parts[i]);
            }
            this.extraText = builder.toString();
        }

        private boolean tryParseCharFlags(String s) {
            int length = s.length();
            if (length < 2 || length > 3) return false;
            this.xCentering = Centering.parseCentering(String.valueOf(s.charAt(0)));
            this.yCentering = Centering.parseCentering(String.valueOf(s.charAt(1)));
            if (length == 3) this.backType = BackType.parseBackType(String.valueOf(s.charAt(2)));
            return true;
        }

        private boolean tryParseSize(String s) {
            if (!s.contains("/") && !s.contains(":")) return false;
            String[] parts = s.split("[/:]");
            float[] values = new float[2];
            try {
                values[0] = Mth.clamp(Float.parseFloat(parts[0]), 1f/32f, 64f);
                values[1] = Mth.clamp(Float.parseFloat(parts[1]), 1f/32f, 64f);
            } catch (Exception ignored) {
                return false;
            }
            this.width = values[0];
            this.height = values[1];
            return true;
        }

        private boolean tryParseXOffset(String s) {
            try {
                this.offsetVec.x = Mth.clamp(Float.parseFloat(s), -64f, 64f);
            } catch (Exception ignored) {
                return false;
            }
            return true;
        }

        private boolean tryParseYOffset(String s) {
            try {
                this.offsetVec.y = Mth.clamp(Float.parseFloat(s), -64f, 64f);
            } catch (Exception ignored) {
                return false;
            }
            return true;
        }

        private boolean tryParseZOffset(String s) {
            try {
                this.offsetVec.z = Mth.clamp(Float.parseFloat(s), -64f, 64f);
            } catch (Exception ignored) {
                return false;
            }
            return true;
        }

        private boolean tryParseRotationVec(String s) {
            try {
                Vector3f vec = getRotVector(s);
                if (vec != null) {
                    this.rotationVec = vec;
                } else {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }
            return true;
        }


        private boolean tryParsePixelsPerBlock(String s) {
            try {
                this.pixelsPerBlock = Mth.clamp(Float.parseFloat(s), 0, 1024);
            } catch (Exception ignored) {
                return false;
            }
            return true;
        }

        public void updateSignText() {
            String urlString = SignedPaintingsClient.imageManager.getShortestURLInference(url);
            String widthString = getShortFloatString(width);
            String heightString = getShortFloatString(height);
            String xOffsetString = getShortFloatString(offsetVec.x);
            String yOffsetString = getShortFloatString(offsetVec.y);
            String zOffsetString = getShortFloatString(offsetVec.z);
            String rotString = getRotString(rotationVec);
            String pixelsPerBlockString = getShortFloatString(pixelsPerBlock);

            String text = urlString + '|' + Centering.getName(true, xCentering) +
                    Centering.getName(false, yCentering) + BackType.getName(backType) + '|' + widthString + ':' +
                    heightString + '|' + yOffsetString + '|' + pixelsPerBlockString + '|' + xOffsetString + '|' +
                    zOffsetString + '|' + rotString; // x and z offset and rotation to the end for backwards compatibility
            String actualText = SignByteMapper.INITIALIZER_STRING + SignByteMapper.encode(text) + SignByteMapper.DELIMITER + extraText;

            SignedPaintingsClient.currentSignEdit.screen.signedPaintings$clear(false);
            int newSelection = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$paste(actualText, 0, 0, true);
            SignedPaintingsClient.currentSignEdit.selectionManager.setSelectionRange(newSelection, newSelection);
        }

        private String getShortFloatString(float value) {
            String s = SignedPaintingsClient.floatToStringDP(value, 5);
            s = s.contains(".") ? s.replaceAll("\\.?0*$","") : s;
            s = s.replaceAll("\\.66[67]+$",".667");
            s = s.replaceAll("\\.333+$",".333");
            return s;
        }

        private String getRotString(Vector3f rotationVec) {
            return getShortFloatString(rotationVec.x) + "," +
                   getShortFloatString(rotationVec.y) + "," +
                   getShortFloatString(rotationVec.z);
        }
    }
}
