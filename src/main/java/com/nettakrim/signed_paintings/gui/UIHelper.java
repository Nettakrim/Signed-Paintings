package com.nettakrim.signed_paintings.gui;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.rendering.BackType;
import com.nettakrim.signed_paintings.rendering.Centering;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.nettakrim.signed_paintings.rendering.SignSideInfo;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class UIHelper {
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 115;
    private static final int PADDING = 10;

    private static final int INPUT_SLIDER_TEXT = 45;
    private static final int INPUT_SLIDER_SLIDER = 64;
    
    private static final int SPACING_X = BUTTON_WIDTH - INPUT_SLIDER_TEXT - INPUT_SLIDER_SLIDER - 1;
    private static final int SPACING_Y = 18;
    private static final int Y_OFF = 55;
    
    private static final int AREA_SIZE = BUTTON_HEIGHT * 2 + 5;

    private static final InputSlider[] inputSliders = new InputSlider[9];
    private static final ArrayList<AbstractWidget> buttons = new ArrayList<>();

    private static Screen screen;

    private static boolean front;
    private static int screenWidth;
    private static boolean aspectLocked = true;
    private static boolean isBackgroundEnabled = false;
    private static float aspectRatio;
    private static PaintingInfo info;
    private static Button backModeButton;
    private static Button untrustButton;
    private static AbstractWidget activeCentering;

    private static Vector3f offsetVec;
    private static Vector3f rotationVec;

    public static void init(boolean isFront, Screen screen, SignBlockEntityAccessor blockEntity) {
        UIHelper.front = isFront;
        UIHelper.screenWidth = screen.width;
        UIHelper.screen = screen;

        buttons.clear();

        float width;
        float height;
        BackType.Type backType;
        float pixelsPerBlock;

        info = front ? blockEntity.signedPaintings$getFrontPaintingInfo() : blockEntity.signedPaintings$getBackPaintingInfo();
        if (info == null) {
            width = 1f;
            height = 1f;
            backType = BackType.Type.SIGN;
            pixelsPerBlock = 0;
            offsetVec = new Vector3f(0, 0, 0);
            rotationVec = new Vector3f(0, 0, 0);
        } else {
            width = info.getWidth();
            height = info.getHeight();
            backType = info.getBackType();
            offsetVec = info.offsetVec;
            rotationVec = info.rotationVec;
            pixelsPerBlock = info.getPixelsPerBlock();
            info.working = true;
        }

        // LEFT
        createCenteringButtons();
        createButton(PADDING, Y_OFF, (BUTTON_WIDTH - SPACING_X) / 2, Component.translatable(SignedPaintingsClient.MODID + ".copy_url"), UIHelper::copyURL);
        createButton(Mth.ceil(PADDING + (BUTTON_WIDTH + SPACING_X) / 2f), Y_OFF, (BUTTON_WIDTH - SPACING_X) / 2, Component.translatable(SignedPaintingsClient.MODID + ".copy_data"), UIHelper::copyData);

        inputSliders[3] = createInputSlider(PADDING, getYPosition(Y_OFF, 1.25f), SignedPaintingsClient.MODID + ".offset_x", -8f, 8f, 0.25f, -64f, 64f, offsetVec.x);
        inputSliders[3].setOnValueChanged(UIHelper::onXOffsetSliderChanged);
        inputSliders[4] = createInputSlider(PADDING, getYPosition(Y_OFF, 2.25f), SignedPaintingsClient.MODID + ".offset_y", -8f, 8f, 0.25f, -64f, 64f, offsetVec.y);
        inputSliders[4].setOnValueChanged(UIHelper::onYOffsetSliderChanged);
        inputSliders[5] = createInputSlider(PADDING, getYPosition(Y_OFF, 3.25f), SignedPaintingsClient.MODID + ".offset_z", -8f, 8f, 0.25f, -64f, 64f, offsetVec.z);
        inputSliders[5].setOnValueChanged(UIHelper::onZOffsetSliderChanged);

        inputSliders[6] = createInputSlider(PADDING, getYPosition(Y_OFF, 4.5f), SignedPaintingsClient.MODID + ".rotation_x", -180f, 180f, 22.5f, -360f, 360f, rotationVec.x);
        inputSliders[6].setOnValueChanged(UIHelper::onXRotationSliderChanged);
        inputSliders[7] = createInputSlider(PADDING, getYPosition(Y_OFF, 5.5f), SignedPaintingsClient.MODID + ".rotation_y", -180f, 180f, 22.5f, -360f, 360f, rotationVec.y);
        inputSliders[7].setOnValueChanged(UIHelper::onYRotationSliderChanged);
        inputSliders[8] = createInputSlider(PADDING, getYPosition(Y_OFF, 6.5f), SignedPaintingsClient.MODID + ".rotation_z", -180f, 180f, 22.5f, -360f, 360f, rotationVec.z);
        inputSliders[8].setOnValueChanged(UIHelper::onZRotationSliderChanged);

        //RIGHT
        inputSliders[0] = createInputSlider(-PADDING, 0, SignedPaintingsClient.MODID + ".size.x", 0.5f, 10f, 0.5f, 1/32f, 64f, width);
        createButton(BUTTON_HEIGHT - PADDING - BUTTON_WIDTH, SPACING_Y, BUTTON_HEIGHT, getAspectLockIcon(aspectLocked), UIHelper::toggleAspectLock);
        createButton(-PADDING, SPACING_Y, BUTTON_WIDTH - BUTTON_HEIGHT - SPACING_X, Component.translatable(SignedPaintingsClient.MODID + ".size.reset"), UIHelper::resetSize);
        inputSliders[1] = createInputSlider(-PADDING, getYPosition(0, 2f), SignedPaintingsClient.MODID + ".size.y", 0.5f, 10f, 0.5f, 1/32f, 64f, height);

        inputSliders[0].setOnValueChanged(value -> onSizeSliderChanged(value, true));
        inputSliders[1].setOnValueChanged(value -> onSizeSliderChanged(value, false));
        aspectRatio = width / height;

        backModeButton = createButton(-PADDING, getYPosition(0, 3.25f), BUTTON_WIDTH, getBackTypeText(backType), UIHelper::cyclePaintingBack);
        inputSliders[2] = createInputSlider(-PADDING, getYPosition(0, 4.25f), SignedPaintingsClient.MODID + ".pixels_per_block", 0, 64, 16, 0, 1024, pixelsPerBlock);
        inputSliders[2].setOnValueChanged(UIHelper::onPixelSliderChanged);

        untrustButton = createButton(-PADDING, getYPosition(Y_OFF, 4.5f), BUTTON_WIDTH, Component.translatable(SignedPaintingsClient.MODID + ".untrust"), UIHelper::untrust);
        createButton(-PADDING, getYPosition(Y_OFF, 5.5f), BUTTON_WIDTH, getBackgroundText(isBackgroundEnabled), UIHelper::cycleBackground);
        createButton(-PADDING, getYPosition(Y_OFF, 6.5f), BUTTON_WIDTH, CommonComponents.GUI_DONE, (Button a) -> screen.onClose());
    }

    private static void createCenteringButtons() {
        Centering.Type[] centering = Centering.Type.values();
        //x centering is reversed to make the buttons have a sensible order when using tab
        for (Centering.Type yCentering : centering) {
            for (int i = 0; i < centering.length; i++) {
                createCenteringButton(centering[centering.length - 1 - i], yCentering);
            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination") // Square button
    private static void createCenteringButton(Centering.Type xCentering, Centering.Type yCentering) {
        String id = (Centering.getName(true, xCentering) + Centering.getName(false, yCentering)).toLowerCase(Locale.ROOT);
        int xPos = getCenteringButtonPosition(AREA_SIZE, xCentering, BUTTON_HEIGHT, 0) + (AREA_SIZE / 2) + (BUTTON_HEIGHT / 2) + PADDING;
        int yPos = -getCenteringButtonPosition(AREA_SIZE, yCentering, BUTTON_HEIGHT, 0) + (AREA_SIZE / 2) - (BUTTON_HEIGHT / 2) + PADDING;
        Button widget = Button.builder(Component.translatable(SignedPaintingsClient.MODID + ".align." + id),
                        button -> {
                            updateActiveCentering(button);
                            getSideInfo().updatePaintingCentering(xCentering, yCentering);
                        })
                .pos(xPos, yPos)
                .size(BUTTON_HEIGHT, BUTTON_HEIGHT)
                .build();

        buttons.add(widget);
    }

    private static int getCenteringButtonPosition(int size, Centering.Type centering, int buttonSize, int screenSize) {
        return Mth.floor(Centering.getOffset(size, centering)) + screenSize / 2 - buttonSize / 2;
    }

    private static int getYPosition(int offset, float count) {
        return offset + Math.round(SPACING_Y * count);
    }

    private static int getAlignedOffset(int offset, int width) {
        if (offset >= 0) {
            return offset;
        }
        return (screenWidth - width) + offset;
    }

    private static Button createButton(int xOffset, int yOffset, int width, Component text, Button.OnPress pressAction) {
        Button button = Button.builder(text, pressAction).pos(getAlignedOffset(xOffset, width), PADDING + yOffset).size(width, BUTTON_HEIGHT).build();
        buttons.add(button);
        return button;
    }

    private static InputSlider createInputSlider(int xOffset, int yOffset, String key, float sliderMin, float sliderMax, float sliderStep, float valueMin, float valueMax, float valueCurrent) {
        InputSlider inputSlider = new InputSlider(getAlignedOffset(xOffset, BUTTON_WIDTH), PADDING + yOffset, INPUT_SLIDER_TEXT, INPUT_SLIDER_SLIDER, BUTTON_HEIGHT, SPACING_X + 1, sliderMin, sliderMax, sliderStep, valueCurrent, valueMin, valueMax, Component.translatable(key));
        buttons.add(inputSlider.sliderWidget);
        buttons.add(inputSlider.textFieldWidget);
        return inputSlider;
    }

    private static void toggleAspectLock(Button button) {
        setAspectLock(!aspectLocked);
        button.setMessage(getAspectLockIcon(aspectLocked));
    }

    private static void setAspectLock(boolean to) {
        aspectLocked = to;
        if (aspectLocked) {
            aspectRatio = inputSliders[0].getValue() / inputSliders[1].getValue();
        }
    }

    private static void resetSize(Button button) {
        SignSideInfo info = getSideInfo();
        info.resetSize();
        inputSliders[0].setValue(info.paintingInfo.getWidth());
        inputSliders[1].setValue(info.paintingInfo.getHeight());
        aspectRatio = inputSliders[0].getValue() / inputSliders[1].getValue();
    }

    private static Component getAspectLockIcon(boolean aspectLocked) {
        return Component.translatable(SignedPaintingsClient.MODID + ".aspect." + (aspectLocked ? "locked" : "unlocked"));
    }

    private static Component getBackTypeText(BackType.Type backType) {
        return Component.translatable(SignedPaintingsClient.MODID + ".back_mode." + (backType.toString().toLowerCase(Locale.ROOT)));
    }

    private static Component getBackgroundText(boolean isEnabled) {
        return Component.translatable(SignedPaintingsClient.MODID + ".background." + (isEnabled ? "y" : "n"));
    }

    private static void cyclePaintingBack(Button button) {
        BackType.Type newType = getSideInfo().cyclePaintingBack();
        button.setMessage(getBackTypeText(newType));
    }

    private static void cycleBackground(Button button) {
        isBackgroundEnabled = !isBackgroundEnabled;
        button.setMessage(getBackgroundText(isBackgroundEnabled));
        SignedPaintingsClient.imageManager.makeChange();
    }

    private static void onSizeSliderChanged(float value, boolean isWidth) {
        if (aspectLocked) {
            if (isWidth) {
                if (aspectRatio > 0) {
                    value /= aspectRatio;
                }
            } else {
                value *= aspectRatio;
            }

            value = SignedPaintingsClient.roundFloatTo3DP(value);

            inputSliders[isWidth ? 1 : 0].setValue(value);
        }
        getSideInfo().updatePaintingSize(inputSliders[0].getValue(), inputSliders[1].getValue());
    }

    private static void onXOffsetSliderChanged(float value) {
        offsetVec.x = value;
        getSideInfo().updatePaintingOffset(offsetVec);
    }

    private static void onYOffsetSliderChanged(float value) {
        offsetVec.y = value;
        getSideInfo().updatePaintingOffset(offsetVec);
    }

    private static void onZOffsetSliderChanged(float value) {
        offsetVec.z = value;
        getSideInfo().updatePaintingOffset(offsetVec);
    }

    private static void onXRotationSliderChanged(float value) {
        rotationVec.x = value;
        getSideInfo().updateRotatingVector(rotationVec);
    }

    private static void onYRotationSliderChanged(float value) {
        rotationVec.y = value;
        getSideInfo().updateRotatingVector(rotationVec);
    }

    private static void onZRotationSliderChanged(float value) {
        rotationVec.z = value;
        getSideInfo().updateRotatingVector(rotationVec);
    }

    private static void onPixelSliderChanged(float value) {
        getSideInfo().updatePaintingPixelsPerBlock(value);
    }

    private static void copyURL(Button button) {
        copyToClipboard(getSideInfo().getUrl());
        screen.onClose();
    }

    private static void copyData(Button button) {
        copyToClipboard(getSideInfo().getData());
        screen.onClose();
    }

    private static void copyToClipboard(String string) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ClipboardManager clipboard = new ClipboardManager();
            clipboard.setClipboard(client.getWindow(), string);
        }
    }

    private static void untrust(Button button) {
        SignedPaintingsClient.imageManager.untrustDomain(SignedPaintingsClient.getDomain(getSideInfo().getUrl()));
        screen.onClose();
    }

    public static ArrayList<AbstractWidget> getButtons() {
        return buttons;
    }

    public static InputSlider[] getInputSliders() {
        return inputSliders;
    }

    public static PaintingInfo getInfo() {
        return info;
    }

    public static boolean isBackgroundEnabled() {
        return isBackgroundEnabled;
    }

    public static void setBackgroundEnabled(boolean to) {
        isBackgroundEnabled = to;
    }

    public static void updateUI(SignSideInfo info) {
        inputSliders[0].setValue(info.paintingInfo.getWidth());
        inputSliders[1].setValue(info.paintingInfo.getHeight());
        inputSliders[2].setValue(info.paintingInfo.getPixelsPerBlock());
        inputSliders[3].setValue(info.paintingInfo.offsetVec.x);
        inputSliders[4].setValue(info.paintingInfo.offsetVec.y);
        inputSliders[5].setValue(info.paintingInfo.offsetVec.z);
        inputSliders[6].setValue(info.paintingInfo.rotationVec.x);
        inputSliders[7].setValue(info.paintingInfo.rotationVec.y);
        inputSliders[8].setValue(info.paintingInfo.rotationVec.z);
        backModeButton.setMessage(getBackTypeText(info.paintingInfo.getBackType()));
        aspectRatio = info.paintingInfo.getWidth() / info.paintingInfo.getHeight();
        untrustButton.setTooltip(Tooltip.create(Component.translatable(SignedPaintingsClient.MODID + ".untrust_info", SignedPaintingsClient.getDomain(info.getUrl()))));
        updateActiveCentering(buttons.get(info.paintingInfo.getCenterIndex()));
    }

    private static void updateActiveCentering(AbstractWidget newCentering) {
        if (activeCentering != null) {
            activeCentering.active = true;
        }
        activeCentering = newCentering;
        activeCentering.active = false;
    }

    public static void addBackground(BackgroundClick backgroundClick) {
        buttons.add(backgroundClick);
    }

    private static SignSideInfo getSideInfo() {
        return SignedPaintingsClient.currentSignEdit.getSideInfo(front);
    }
}
