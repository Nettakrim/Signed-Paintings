package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.*;
import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.gui.BackgroundClick;
import com.nettakrim.signed_paintings.gui.InputSlider;
import com.nettakrim.signed_paintings.gui.SignEditingInfo;
import com.nettakrim.signed_paintings.rendering.BackType;
import com.nettakrim.signed_paintings.rendering.Centering;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.nettakrim.signed_paintings.rendering.SignSideInfo;
import com.nettakrim.signed_paintings.util.ImageManager;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Locale;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements AbstractSignEditScreenAccessor {
    @Shadow
    private SignText text;

    @Final
    @Shadow
    private String[] messages;

    @Final
    @Shadow
    private SignBlockEntity blockEntity;

    @Final
    @Shadow
    private boolean front;

    @Shadow
    private int currentRow;

    @Shadow
    private SelectionManager selectionManager;

    @Unique
    private final InputSlider[] inputSliders = new InputSlider[4];

    @Unique
    private final ArrayList<ClickableWidget> buttons = new ArrayList<>();

    @Unique
    private ClickableWidget uploadButton;

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected abstract void setCurrentRowMessage(String message);

    @Unique
    private boolean aspectLocked = true;

    @Unique
    private float aspectRatio;

    @Unique
    private String uploadURL = null;

    @Inject(at = @At("TAIL"), method = "init")
    private void init(CallbackInfo ci) {
        buttons.clear();

        Centering.Type[] centering = Centering.Type.values();
        //x centering is reversed to make the buttons have a sensible order when using tab
        Centering.Type[] reversedCentering = new Centering.Type[] {
            Centering.Type.MAX,
            Centering.Type.CENTER,
            Centering.Type.MIN
        };
        for (Centering.Type yCentering : centering) {
            for (Centering.Type xCentering : reversedCentering) {
                createCenteringButton(51, 20, xCentering, yCentering);
            }
        }

        float width;
        float height;
        BackType.Type backType;
        float yOffset;
        float pixelsPerBlock;

        SignBlockEntityAccessor sign = (SignBlockEntityAccessor)blockEntity;
        PaintingInfo info = front ? sign.signedPaintings$getFrontPaintingInfo() : sign.signedPaintings$getBackPaintingInfo();
        if (info == null) {
            width = 1f;
            height = 1f;
            backType = BackType.Type.SIGN;
            yOffset = 0;
            pixelsPerBlock = 0;
        } else {
            width = info.getWidth();
            height = info.getHeight();
            backType = info.getBackType();
            yOffset = info.getYOffset();
            pixelsPerBlock = info.getPixelsPerBlock();
            info.working = true;
        }

        inputSliders[2] = createYOffsetSlider(76, 50, 50, 20, 5, SignedPaintingsClient.MODID+".offset_y", yOffset);
        inputSliders[2].setOnValueChanged(this::onYOffsetSliderChanged);

        inputSliders[0] = createSizingSlider(Centering.Type.MAX, 51, 50, 50, 20, 5, SignedPaintingsClient.MODID+".size.x", width);
        createLockingButton(Centering.Type.CENTER, 51, 20, getAspectLockIcon(aspectLocked));
        createResetButton(Centering.Type.CENTER, 51, 80, 20, Text.translatable(SignedPaintingsClient.MODID+".size.reset"));
        inputSliders[1] = createSizingSlider(Centering.Type.MIN, 51, 50, 50, 20, 5, SignedPaintingsClient.MODID+".size.y", height);

        inputSliders[0].setOnValueChanged(value -> onSizeSliderChanged(value, true));
        inputSliders[1].setOnValueChanged(value -> onSizeSliderChanged(value, false));
        aspectRatio = width / height;

        inputSliders[3] = createPixelSlider(101, 50, 50, 20, 5, SignedPaintingsClient.MODID+".pixels_per_block", pixelsPerBlock);
        inputSliders[3].setOnValueChanged(this::onPixelSliderChanged);

        createBackModeButton(76, 105, 20, backType);

        uploadButton = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID+".upload_prompt"), this::upload).dimensions(this.width / 2 - 100, (this.height / 4 + 144) - 25, 200, 20).build();
        addDrawableChild(uploadButton);
        addSelectableChild(uploadButton);
        if (uploadURL == null) uploadButton.visible = false;

        BackgroundClick backgroundClick = new BackgroundClick(inputSliders);
        addSelectableChild(backgroundClick);
        buttons.add(backgroundClick);

        SignedPaintingsClient.currentSignEdit.setSelectionManager(selectionManager);

        if (info == null || !info.isReady()) {
            signedPaintings$setVisibility(false);
        }
    }

    @Unique
    private void createCenteringButton(int areaSize, int buttonSize, Centering.Type xCentering, Centering.Type yCentering) {
        String id = (Centering.getName(true, xCentering)+Centering.getName(false, yCentering)).toLowerCase(Locale.ROOT);
        ButtonWidget widget = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID+".align."+id), button -> SignedPaintingsClient.currentSignEdit.getSideInfo(front).updatePaintingCentering(xCentering, yCentering))
        .position(getCenteringButtonPosition(areaSize, xCentering, buttonSize, width)-(areaSize/2)-(buttonSize/2)-60, getCenteringButtonPosition(-areaSize, yCentering, buttonSize, 0)+(areaSize/2)+(buttonSize/2)+67)
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
    }

    @Unique
    private void createBackModeButton(int yOffset, int buttonWidth, int buttonHeight, BackType.Type backType) {
        ButtonWidget widget = ButtonWidget.builder(getBackTypeText(backType), this::cyclePaintingBack)
                .position((width/2)+60, yOffset+68)
                .size(buttonWidth, buttonHeight)
                .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
    }

    @Unique
    private int getCenteringButtonPosition(int size, Centering.Type centering, int buttonSize, int screenSize) {
        return MathHelper.floor(Centering.getOffset(size, centering)) + screenSize/2 - buttonSize/2;
    }

    @Unique
    private InputSlider createSizingSlider(Centering.Type centering, int areaSize, int textWidth, int sliderWidth, int widgetHeight, int elementSpacing, String key, float startingValue) {
        int x = (width/2)+60;
        int y = getCenteringButtonPosition(areaSize, centering, widgetHeight, 0)+(areaSize/2)+(widgetHeight/2)+67;
        InputSlider inputSlider = new InputSlider(x, y, textWidth, sliderWidth, widgetHeight, elementSpacing, 0.5f, 10f, 0.5f, startingValue, 1/32f, 64f, Text.translatable(key));

        addDrawableChild(inputSlider.sliderWidget);
        addSelectableChild(inputSlider.sliderWidget);
        buttons.add(inputSlider.sliderWidget);

        addDrawableChild(inputSlider.textFieldWidget);
        addSelectableChild(inputSlider.textFieldWidget);
        buttons.add(inputSlider.textFieldWidget);

        return inputSlider;
    }

    @Unique
    private void createLockingButton(Centering.Type centering, int areaSize, int buttonSize, Text text) {
        ButtonWidget widget = ButtonWidget.builder(text, this::toggleAspectLock)
        .position((width/2)+60, getCenteringButtonPosition(areaSize, centering, buttonSize, 0)+(areaSize/2)+(buttonSize/2)+67)
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
    }

    @Unique
    private void createResetButton(Centering.Type centering, int areaSize, int buttonWidth, int buttonHeight, Text text) {
        ButtonWidget widget = ButtonWidget.builder(text, this::resetSize)
                .position((width/2)+60+25, getCenteringButtonPosition(areaSize, centering, buttonHeight, 0)+(areaSize/2)+(buttonHeight/2)+67)
                .size(buttonWidth, buttonHeight)
                .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
    }

    @Unique
    private InputSlider createYOffsetSlider(int yOffset, int textWidth, int sliderWidth, int widgetHeight, int elementSpacing, String key, float startingValue) {
        int x = (width/2)-60-(textWidth+sliderWidth+elementSpacing);
        int y = yOffset+68;
        InputSlider inputSlider = new InputSlider(x, y, textWidth, sliderWidth, widgetHeight, elementSpacing, -8f, 8f, 1f, startingValue, -64f, 64f, Text.translatable(key));

        addDrawableChild(inputSlider.sliderWidget);
        addSelectableChild(inputSlider.sliderWidget);
        buttons.add(inputSlider.sliderWidget);

        addDrawableChild(inputSlider.textFieldWidget);
        addSelectableChild(inputSlider.textFieldWidget);
        buttons.add(inputSlider.textFieldWidget);

        return inputSlider;
    }

    @Unique
    private InputSlider createPixelSlider(int yOffset, int textWidth, int sliderWidth, int widgetHeight, int elementSpacing, String key, float startingValue) {
        int x = (width/2)+60;
        int y = yOffset+68;
        InputSlider inputSlider = new InputSlider(x, y, textWidth, sliderWidth, widgetHeight, elementSpacing, 0, 64, 16, startingValue, 0, 1024f, Text.translatable(key));

        addDrawableChild(inputSlider.sliderWidget);
        addSelectableChild(inputSlider.sliderWidget);
        buttons.add(inputSlider.sliderWidget);

        addDrawableChild(inputSlider.textFieldWidget);
        addSelectableChild(inputSlider.textFieldWidget);
        buttons.add(inputSlider.textFieldWidget);

        return inputSlider;
    }

    @Unique
    private void toggleAspectLock(ButtonWidget button) {
        setAspectLock(!aspectLocked);
        button.setMessage(getAspectLockIcon(aspectLocked));
    }

    @Unique
    private void setAspectLock(boolean to) {
        aspectLocked = to;
        if (aspectLocked) {
            aspectRatio = inputSliders[0].getValue() / inputSliders[1].getValue();
        }
    }

    @Unique
    private void resetSize(ButtonWidget button) {
        SignSideInfo info = SignedPaintingsClient.currentSignEdit.getSideInfo(front);
        info.resetSize();
        inputSliders[0].setValue(info.paintingInfo.getWidth());
        inputSliders[1].setValue(info.paintingInfo.getHeight());
        aspectRatio = inputSliders[0].getValue() / inputSliders[1].getValue();
    }

    @Unique
    private static Text getAspectLockIcon(boolean aspectLocked) {
        return Text.translatable(SignedPaintingsClient.MODID+".aspect."+ (aspectLocked ? "locked" : "unlocked"));
    }

    @Unique
    private static Text getBackTypeText(BackType.Type backType) {
        return Text.translatable(SignedPaintingsClient.MODID+".back_mode."+(backType.toString().toLowerCase(Locale.ROOT)));
    }

    @Unique
    private void cyclePaintingBack(ButtonWidget button) {
        BackType.Type newType = SignedPaintingsClient.currentSignEdit.getSideInfo(front).cyclePaintingBack();
        button.setMessage(getBackTypeText(newType));
    }

    @Unique
    private void onSizeSliderChanged(float value, boolean isWidth) {
        if (aspectLocked) {
            if (isWidth) value/=aspectRatio;
            else value*=aspectRatio;

            value = SignedPaintingsClient.roundFloatTo3DP(value);

            inputSliders[isWidth ? 1 : 0].setValue(value);
        }
        SignedPaintingsClient.currentSignEdit.getSideInfo(front).updatePaintingSize(inputSliders[0].getValue(), inputSliders[1].getValue());
    }

    @Unique
    private void onYOffsetSliderChanged(float value) {
        SignedPaintingsClient.currentSignEdit.getSideInfo(front).updatePaintingYOffset(value);
    }

    @Unique
    private void onPixelSliderChanged(float value) {
        SignedPaintingsClient.currentSignEdit.getSideInfo(front).updatePaintingPixelsPerBlock(value);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V")
    private void onScreenOpen(SignBlockEntity blockEntity, boolean front, boolean filtered, Text title, CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = new SignEditingInfo(blockEntity, this);
    }

    @Inject(at = @At("TAIL"), method = "finishEditing")
    private void onScreenClose(CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = null;
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    private void onKeyPress(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : inputSliders) {
            if (slider != null && slider.isFocused() && slider.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "charTyped", cancellable = true)
    private void onCharType(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : inputSliders) {
            if (slider != null && slider.isFocused() && slider.charTyped(chr, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @ModifyVariable(at = @At("STORE"), method = "renderSignText", ordinal = 0)
    private boolean stopTextCaret(boolean bl) {
        for (InputSlider slider : inputSliders) {
            if (slider != null && slider.isFocused()) {
                selectionManager.setSelectionEnd(selectionManager.getSelectionStart());
                return false;
            }
        }
        return bl;
    }

    @Override
    public void signedPaintings$clear() {
        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = "";
            this.text = this.text.withMessage(i, Text.literal("message"));
        }
        this.blockEntity.setText(this.text, this.front);
        this.currentRow = 0;
    }

    @Override
    public int signedPaintings$paste(String pasteString, int selectionStart, int selectionEnd) {
        String[] newMessages = new String[messages.length];
        System.arraycopy(messages, 0, newMessages, 0, messages.length);

        int maxWidthPerLine = this.blockEntity.getMaxTextWidth();
        TextRenderer textRenderer = SignedPaintingsClient.client.textRenderer;

        selectionStart = MathHelper.clamp(selectionStart, 0, newMessages[currentRow].length());
        selectionEnd = MathHelper.clamp(selectionEnd, 0, newMessages[currentRow].length());
        newMessages[currentRow] = newMessages[currentRow].substring(0,selectionStart)+pasteString+newMessages[currentRow].substring(selectionEnd);
        int currentWidth = textRenderer.getWidth(newMessages[currentRow]);

        int cursor = selectionStart+pasteString.length();

        if (ImageManager.isValid(pasteString)) {
            if (textRenderer.getWidth(pasteString) > maxWidthPerLine*2.5) {
                uploadURL = pasteString;
                uploadButton.visible = true;
            }
        }

        if (currentWidth < maxWidthPerLine) {
            setCurrentRowMessage(newMessages[currentRow]);
            return cursor;
        }

        int cursorRow = currentRow;

        while (true) {
            String line = newMessages[currentRow];
            int index = SignedPaintingsClient.getMaxFittingIndex(line, maxWidthPerLine, textRenderer);
            newMessages[currentRow] = line.substring(0, index);
            if (currentRow == messages.length-1 || line.length() <= index) {
                break;
            }
            if (currentRow == cursorRow && cursor > index) {
                cursorRow++;
                cursor -= index;
            }
            currentRow++;
            newMessages[currentRow] = line.substring(index)+newMessages[currentRow];
        }
        cursor = MathHelper.clamp(cursor, 0, newMessages[cursorRow].length());

        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = newMessages[i];
            this.text = this.text.withMessage(i, Text.literal(this.messages[i]));
        }
        this.blockEntity.setText(this.text, this.front);

        currentRow = cursorRow;
        return cursor;
    }

    @Unique
    private void upload(ButtonWidget button) {
        if (uploadURL == null) return;
        SignedPaintingsClient.uploadManager.uploadToImgur(uploadURL, this::uploadFinished);
    }

    @Unique
    private void uploadFinished(String link) {
        if (!SignedPaintingsClient.currentSignEdit.sign.equals(blockEntity)) {
            return;
        }
        if (link == null) {
            uploadButton.setMessage(Text.translatable(SignedPaintingsClient.MODID+".upload_fail"));
            uploadURL = null;
            return;
        }
        uploadButton.visible = false;
        signedPaintings$clear();
        signedPaintings$paste(link, 0, 0);
    }

    @Override
    public void signedPaintings$setVisibility(boolean to) {
        for (ClickableWidget clickableWidget : buttons) {
            clickableWidget.visible = to;
        }
    }

    @Override
    public void signedPaintings$initSliders(float width, float height) {
        inputSliders[0].setValue(width);
        inputSliders[1].setValue(height);
        aspectRatio = width / height;
    }
}
