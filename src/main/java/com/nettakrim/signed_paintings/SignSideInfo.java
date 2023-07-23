package com.nettakrim.signed_paintings;

import net.minecraft.block.entity.SignText;
import net.minecraft.util.Identifier;

public class SignSideInfo {
    public SignText text;
    public PaintingInfo paintingInfo;

    public SignSideInfo(SignText text, PaintingInfo paintingInfo) {
        this.text = text;
        this.paintingInfo = paintingInfo;
    }

    public void loadPainting(Identifier back, boolean isFront, boolean isWall) {
        String combinedText = SignedPaintingsClient.combineSignText(text);
        String[] parts = combinedText.split(" ", 2);
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

        //this should probably be done through regex groups, to cleanly extract positioning, sizing data etc. regardless of order?
        SignedPaintingsClient.LOGGER.info("loading extra data \""+afterURL+"\"");
        if (afterURL.length() > 1) {
            String xCentering = afterURL.substring(0,1);
            String yCentering = afterURL.substring(1,2);
            paintingInfo.updateCuboidCentering(Cuboid.getCenteringFromName(xCentering), Cuboid.getCenteringFromName(yCentering));
        }
    }

    public void updatePaintingCentering(Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        paintingInfo.updateCuboidCentering(xCentering, yCentering);

        String centering = Cuboid.getNameFromCentering(true, xCentering)+Cuboid.getNameFromCentering(false, yCentering);

        String newText = SignedPaintingsClient.combineSignText(text);

        int splitStart = newText.indexOf(' ');
        int splitEnd;

        if (splitStart == -1) {
            splitStart = newText.length();
            splitEnd = splitStart;
            centering = " "+centering;
        } else {
            splitStart++;
            splitEnd = splitStart+2;
        }

        newText = newText.substring(0, splitStart)+centering+newText.substring(splitEnd);

        SignedPaintingsClient.currentSignEdit.screen.signedPaintings$clear();
        int newSelection = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$paste(newText, 0, 0);
        SignedPaintingsClient.currentSignEdit.selectionManager.setSelection(newSelection, newSelection);
    }

    public void updatePaintingSize(float xSize, float ySize) {
        paintingInfo.updateCuboidSize(xSize, ySize);
    }
}
