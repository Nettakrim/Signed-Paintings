package com.nettakrim;

import net.minecraft.block.entity.SignText;
import net.minecraft.util.Identifier;

public class SignSideData {
    public SignText text;
    public PaintingInfo paintingInfo;

    public SignSideData(SignText text, PaintingInfo paintingInfo) {
        this.text = text;
        this.paintingInfo = paintingInfo;
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

        //for some reason combineSignText(text) does not work?
        String newText = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$getCombinedMessage();
        SignedPaintingsClient.currentSignEdit.screen.signedPaintings$clear();

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

        int newSelection = SignedPaintingsClient.currentSignEdit.screen.signedPaintings$paste(newText, 0, 0);
        SignedPaintingsClient.currentSignEdit.selectionManager.setSelection(newSelection, newSelection);
    }

    public void updatePaintingSize(float xSize, float ySize) {
        paintingInfo.updateCuboidSize(xSize, ySize);
    }
}
