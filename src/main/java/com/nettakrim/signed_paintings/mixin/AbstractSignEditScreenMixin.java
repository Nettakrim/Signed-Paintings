package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.*;
import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected abstract void setCurrentRowMessage(String message);

    @Unique
    private boolean aspectLocked;

    @Inject(at = @At("TAIL"), method = "init")
    private void init(CallbackInfo ci) {
        //🡤🡡🡥🡠◯🡢🡧🡣🡦
        String[] arrows = new String[] {"\uD83E\uDC64","\uD83E\uDC61","\uD83E\uDC65","\uD83E\uDC60","◯","\uD83E\uDC62","\uD83E\uDC67","\uD83E\uDC63","\uD83E\uDC66"};

        Cuboid.Centering[] centering = Cuboid.Centering.values();
        Cuboid.Centering[] reversedCentering = new Cuboid.Centering[] {
            Cuboid.Centering.MAX,
            Cuboid.Centering.CENTER,
            Cuboid.Centering.MIN
        };
        for (Cuboid.Centering yCentering : centering) {
            for (Cuboid.Centering xCentering : reversedCentering) {
                int arrowIndex = 2-xCentering.ordinal() + yCentering.ordinal()*3;
                createCenteringButton(50, 20, arrows[arrowIndex], xCentering, yCentering);
            }
        }

        inputSliders[0] = createSizingSlider(Cuboid.Centering.MAX, 50, 30, 100, 20, 5, "Width", 2f);
        createLockingButton(Cuboid.Centering.CENTER, 50, 20, getAspectLockIcon(aspectLocked));
        inputSliders[1] = createSizingSlider(Cuboid.Centering.MIN, 50, 30, 100, 20, 5, "Height", 3f);

        addSelectableChild(new BackgroundClick(inputSliders));

        SignedPaintingsClient.currentSignEdit.setSelectionManager(selectionManager);
    }

    @Unique
    private void createCenteringButton(int areaSize, int buttonSize, String text, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        ButtonWidget widget = ButtonWidget.builder(Text.literal(text), button -> SignedPaintingsClient.currentSignEdit.updatePaintingCentering(front, xCentering, yCentering))
        .position(getCenteringButtonPosition(areaSize, xCentering, buttonSize, width)-(width/4), getCenteringButtonPosition(-areaSize, yCentering, buttonSize, height))
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
    }

    @Unique
    private int getCenteringButtonPosition(int size, Cuboid.Centering centering, int buttonSize, int screenSize) {
        return MathHelper.floor(Cuboid.getOffsetFromCentering(size, centering)) + screenSize/2 - buttonSize/2;
    }

    @Unique
    private InputSlider createSizingSlider(Cuboid.Centering centering, int areaSize, int textWidth, int sliderWidth, int widgetHeight, int elementSpacing, String name, float startingValue) {
        int x = (width/2 + width/4)-(textWidth+sliderWidth+elementSpacing)/2;
        int y = getCenteringButtonPosition(areaSize, centering, widgetHeight, height);
        InputSlider inputSlider = new InputSlider(x, y, textWidth, sliderWidth, widgetHeight, elementSpacing, 0.5f, 10f, 0.5f, startingValue, Text.literal(name));

        addDrawableChild(inputSlider.textFieldWidget);
        addSelectableChild(inputSlider.textFieldWidget);

        addDrawableChild(inputSlider.sliderWidget);
        addSelectableChild(inputSlider.sliderWidget);

        return inputSlider;
    }

    @Unique
    private void createLockingButton(Cuboid.Centering centering, int areaSize, int buttonSize, String text) {
        ButtonWidget widget = ButtonWidget.builder(Text.literal(text), this::toggleAspectLock)
        .position((width/2 + width/4)-(areaSize)/2, getCenteringButtonPosition(areaSize, centering, buttonSize, height))
        .size(buttonSize, buttonSize)
        .build();

        addDrawableChild(widget);
        addSelectableChild(widget);
    }

    @Unique
    private void toggleAspectLock(ButtonWidget button) {
        aspectLocked = !aspectLocked;
        button.setMessage(Text.literal(getAspectLockIcon(aspectLocked)));
    }

    @Unique
    private static String getAspectLockIcon(boolean aspectLocked) {
        // "🔒" : "🔓"
        return aspectLocked ? "\uD83D\uDD12" : "\uD83D\uDD13";
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
}
