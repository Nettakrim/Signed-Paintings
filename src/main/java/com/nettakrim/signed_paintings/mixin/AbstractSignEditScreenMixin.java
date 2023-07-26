package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.*;
import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final InputSlider[] inputSliders = new InputSlider[2];

    @Unique
    private final ArrayList<ClickableWidget> buttons = new ArrayList<>();

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected abstract void setCurrentRowMessage(String message);

    @Unique
    private boolean aspectLocked = true;

    @Unique
    private float aspectRatio;

    @Inject(at = @At("TAIL"), method = "init")
    private void init(CallbackInfo ci) {
        buttons.clear();

        Cuboid.Centering[] centering = Cuboid.Centering.values();
        //x centering is reversed to make the buttons have a sensible order when using tab
        Cuboid.Centering[] reversedCentering = new Cuboid.Centering[] {
            Cuboid.Centering.MAX,
            Cuboid.Centering.CENTER,
            Cuboid.Centering.MIN
        };
        for (Cuboid.Centering yCentering : centering) {
            for (Cuboid.Centering xCentering : reversedCentering) {
                createCenteringButton(51, 20, xCentering, yCentering);
            }
        }

        float width;
        float height;

        SignBlockEntityAccessor sign = (SignBlockEntityAccessor)blockEntity;
        PaintingInfo info = front ? sign.signedPaintings$getFrontPaintingInfo() : sign.signedPaintings$getBackPaintingInfo();
        if (info == null) {
            width = 1f;
            height = 1f;
        } else {
            width = info.getWidth();
            height = info.getHeight();
        }

        inputSliders[0] = createSizingSlider(Cuboid.Centering.MAX, 51, 50, 50, 20, 5, SignedPaintingsClient.MODID+".size.x", width);
        createLockingButton(Cuboid.Centering.CENTER, 51, 20, getAspectLockIcon(aspectLocked));
        inputSliders[1] = createSizingSlider(Cuboid.Centering.MIN, 51, 50, 50, 20, 5, SignedPaintingsClient.MODID+".size.y", height);

        inputSliders[0].setOnValueChanged(value -> onSizeSliderChanged(value, true));
        inputSliders[1].setOnValueChanged(value -> onSizeSliderChanged(value, false));
        aspectRatio = width / height;

        BackgroundClick backgroundClick = new BackgroundClick(inputSliders);
        addSelectableChild(backgroundClick);
        buttons.add(backgroundClick);

        SignedPaintingsClient.currentSignEdit.setSelectionManager(selectionManager);

        if (info == null || !info.isReady()) {
            signedPaintings$setVisibility(false);
        }
    }

    @Unique
    private void createCenteringButton(int areaSize, int buttonSize, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        String id = (Cuboid.getNameFromCentering(true, xCentering)+Cuboid.getNameFromCentering(false, yCentering)).toLowerCase(Locale.ROOT);
        ButtonWidget widget = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID+".align."+id), button -> SignedPaintingsClient.currentSignEdit.updatePaintingCentering(front, xCentering, yCentering))
        //.position(getCenteringButtonPosition(areaSize, xCentering, buttonSize, width)-(width/4), getCenteringButtonPosition(-areaSize, yCentering, buttonSize, height))
        .position(getCenteringButtonPosition(areaSize, xCentering, buttonSize, width)-(areaSize/2)-(buttonSize/2)-60, getCenteringButtonPosition(-areaSize, yCentering, buttonSize, 0)+(areaSize/2)+(buttonSize/2)+67)
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
    }

    @Unique
    private int getCenteringButtonPosition(int size, Cuboid.Centering centering, int buttonSize, int screenSize) {
        return MathHelper.floor(Cuboid.getOffsetFromCentering(size, centering)) + screenSize/2 - buttonSize/2;
    }

    @Unique
    private InputSlider createSizingSlider(Cuboid.Centering centering, int areaSize, int textWidth, int sliderWidth, int widgetHeight, int elementSpacing, String key, float startingValue) {
        //int x = (width/2 + width/4)-(textWidth+sliderWidth+elementSpacing)/2;
        int x = (width/2)+60;
        int y = getCenteringButtonPosition(areaSize, centering, widgetHeight, 0)+(areaSize/2)+(widgetHeight/2)+67;
        InputSlider inputSlider = new InputSlider(x, y, textWidth, sliderWidth, widgetHeight, elementSpacing, 0.5f, 10f, 0.5f, startingValue, Text.translatable(key));

        addDrawableChild(inputSlider.sliderWidget);
        addSelectableChild(inputSlider.sliderWidget);
        buttons.add(inputSlider.sliderWidget);

        addDrawableChild(inputSlider.textFieldWidget);
        addSelectableChild(inputSlider.textFieldWidget);
        buttons.add(inputSlider.textFieldWidget);

        return inputSlider;
    }

    @Unique
    private void createLockingButton(Cuboid.Centering centering, int areaSize, int buttonSize, Text text) {
        ButtonWidget widget = ButtonWidget.builder(text, this::toggleAspectLock)
        .position((width/2)+60, getCenteringButtonPosition(areaSize, centering, buttonSize, 0)+(areaSize/2)+(buttonSize/2)+67)
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
        buttons.add(widget);
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
    private static Text getAspectLockIcon(boolean aspectLocked) {
        return Text.translatable(SignedPaintingsClient.MODID+".aspect."+ (aspectLocked ? "locked" : "unlocked"));
    }

    @Unique
    private void onSizeSliderChanged(float value, boolean isWidth) {
        if (aspectLocked) {
            if (isWidth) value/=aspectRatio;
            else value*=aspectRatio;

            //silly conversions to try get rid of awkwardly long numbers like 1.499 or 3.002
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(3, RoundingMode.HALF_UP);
            BigDecimal bd2 = bd.setScale(2, RoundingMode.HALF_UP);
            double difference = Math.abs(bd.subtract(bd2).doubleValue());
            String s = bd.toString();
            if ((difference < 0.0011 || s.contains("00")) && !s.endsWith(".667") && !s.endsWith(".334")) {
                value = bd2.floatValue();
            } else {
                value = bd.floatValue();
            }

            inputSliders[isWidth ? 1 : 0].setValue(value);
        }
        SignedPaintingsClient.currentSignEdit.updatePaintingSize(front, inputSliders[0].getValue(), inputSliders[1].getValue());
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
            if (slider.isFocused() && slider.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "charTyped", cancellable = true)
    private void onCharType(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : inputSliders) {
            if (slider.isFocused() && slider.charTyped(chr, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @ModifyVariable(at = @At("STORE"), method = "renderSignText", ordinal = 0)
    private boolean stopTextCaret(boolean bl) {
        for (InputSlider slider : inputSliders) {
            if (slider.isFocused()) {
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
