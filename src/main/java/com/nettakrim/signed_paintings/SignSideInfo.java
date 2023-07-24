package com.nettakrim.signed_paintings;

import net.minecraft.block.entity.SignText;
import net.minecraft.util.Identifier;

public class SignSideInfo {
    public SignText text;
    public PaintingInfo paintingInfo;

    private PaintingDataCache cache;

    public SignSideInfo(SignText text, PaintingInfo paintingInfo) {
        this.text = text;
        this.paintingInfo = paintingInfo;
    }

    public void loadPainting(Identifier back, boolean isFront, boolean isWall) {
        String combinedText = SignedPaintingsClient.combineSignText(text);
        String[] parts = combinedText.split("[:\n ]", 2);
        cache = new PaintingDataCache(parts[0]);
        String url = SignedPaintingsClient.imageManager.applyURLInferences(parts[0]);
        loadURL(url, parts.length > 1 ? parts[1] : "", back, isFront, isWall);
    }

    private void loadURL(String url, String afterURL, Identifier back, boolean isFront, boolean isWall) {
        if (paintingInfo != null) paintingInfo.invalidateImage();
        SignedPaintingsClient.imageManager.loadImage(url, (data) -> updateInfo(data, afterURL, back, isFront, isWall));
    }

    public void updateInfo(ImageData data, String afterURL, Identifier back, boolean isFront, boolean isWall) {
        SignedPaintingsClient.LOGGER.info("updating painting info for "+data.identifier);
        if (paintingInfo == null) {
            paintingInfo = new PaintingInfo(data, back, isFront, isWall);
        } else {
            paintingInfo.updateImage(data);
        }

        cache.initFromImageData(data);

        SignedPaintingsClient.LOGGER.info("loading extra data \""+afterURL+"\"");
        updateCache(afterURL);
    }

    public void updatePaintingCentering(Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        paintingInfo.updateCuboidCentering(xCentering, yCentering);
        cache.xCentering = xCentering;
        cache.yCentering = yCentering;
        cache.updateSignText();
    }

    public void updatePaintingSize(float xSize, float ySize) {
        SignedPaintingsClient.LOGGER.info("setting size to "+xSize+" "+ySize);
        paintingInfo.updateCuboidSize(xSize, ySize);
        cache.width = xSize;
        cache.height = ySize;
        cache.updateSignText();
    }

    public void updateText() {
        if (paintingInfo == null) return;
        String combinedText = SignedPaintingsClient.combineSignText(text);
        String[] parts = combinedText.split("[:\n ]", 2);
        cache.url = parts[0];
        if (parts.length > 1) updateCache(parts[1]);
    }

    private void updateCache(String afterUrl) {
        cache.parseAfterUrl(afterUrl);

        paintingInfo.updateCuboidCentering(cache.xCentering, cache.yCentering);
        paintingInfo.updateCuboidSize(cache.width, cache.height);
    }

    private static class PaintingDataCache {
        private String url;
        private Cuboid.Centering xCentering;
        private Cuboid.Centering yCentering;
        private float width;
        private float height;
        private String extraText;

        public PaintingDataCache(String url) {
            this.url = url;
        }

        public void initFromImageData(ImageData imageData) {
            this.xCentering = Cuboid.Centering.CENTER;
            this.yCentering = Cuboid.Centering.CENTER;

            this.width = imageData.width/16f;
            this.height = imageData.height/16f;
            while (this.width > 8 || this.height > 8) {
                this.width /= 2f;
                this.height /= 2f;
            }
        }

        public void parseAfterUrl(String s) {
            String[] parts = s.split("[:\n ]");

            int currentIndex = 0;

            if (currentIndex < parts.length && tryParseCentering(parts[currentIndex])) currentIndex++;

            if (currentIndex < parts.length && tryParseSize(parts[currentIndex])) currentIndex++;

            StringBuilder builder = new StringBuilder();
            for (int i = currentIndex; i < parts.length; i++) {
                if (builder.length() > 0) builder.append(":");
                builder.append(parts[i]);
            }
            this.extraText = builder.toString();
        }

        private boolean tryParseCentering(String s) {
            SignedPaintingsClient.LOGGER.info(s);
            if (s.length() != 2) return false;
            this.xCentering = Cuboid.getCenteringFromName(String.valueOf(s.charAt(0)));
            this.yCentering = Cuboid.getCenteringFromName(String.valueOf(s.charAt(1)));
            return true;
        }

        private boolean tryParseSize(String s) {
            if (!s.contains("/")) return false;
            String[] parts = s.split("/");
            float[] values = new float[2];
            try {
                values[0] = Float.parseFloat(parts[0]);
                values[1] = Float.parseFloat(parts[1]);
            } catch (NumberFormatException ignored) {
                return false;
            }
            this.width = values[0];
            this.height = values[1];
            return true;
        }

        public void updateSignText() {
            String text = url + ':' + Cuboid.getNameFromCentering(true, xCentering) + Cuboid.getNameFromCentering(false, yCentering) + ':' + width + '/' + height + ':' + extraText;

            SignedPaintingsClient.currentSignEdit.screen.signedPaintings$clear();
            int newSelection = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$paste(text, 0, 0);
            SignedPaintingsClient.currentSignEdit.selectionManager.setSelection(newSelection, newSelection);
        }
    }
}
