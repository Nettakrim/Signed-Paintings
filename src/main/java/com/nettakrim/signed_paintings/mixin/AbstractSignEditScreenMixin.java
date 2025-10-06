package com.nettakrim.signed_paintings.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.gui.*;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.nettakrim.signed_paintings.rendering.SignSideInfo;
import com.nettakrim.signed_paintings.util.ImageManager;
import com.nettakrim.signed_paintings.util.SignByteMapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements AbstractSignEditScreenAccessor {

    @Shadow
    private SignText text;

    @Final
    @Shadow
    private String[] messages;

    @Final
    @Shadow
    protected SignBlockEntity blockEntity;

    @Final
    @Shadow
    private boolean front;

    @Shadow
    private int currentRow;

    @Shadow
    private SelectionManager selectionManager;

    @Unique
    private String url = null;

    @Unique
    private String domain = null;

    @Unique
    private ClickableWidget uploadButton;

    @Unique
    private ClickableWidget doneButton;

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract void setCurrentRowMessage(String message);

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private boolean shouldRenderTitle(DrawContext context, TextRenderer textRenderer, Text text, int centerX, int y, int color){
        return !isInfoCorrect();
    }

    @WrapOperation(method = "renderSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;translateForRender(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/block/BlockState;)V"))
    private void translateForRender(AbstractSignEditScreen instance, DrawContext context, BlockState blockState, Operation<Void> original){
        if (isInfoCorrect()) {
            //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
            if (this.getClass().equals(SignEditScreen.class)) {
                boolean bl = blockState.getBlock() instanceof SignBlock;
                if (bl) {
                    context.getMatrices().translate(0.0f, -16.0f, 0f);
                } else {
                    context.getMatrices().translate(0.0f, -4.0f, 0f);
                }
            }
            // 97.5 is centered, but it looks a bit weird, deliberately offcentering it ends up looking better
            context.getMatrices().translate(86.5f, 38.0f, 50.0f);
            context.getMatrices().scale(0.5f, 0.5f, 0.5f);
        } else {
            original.call(instance, context, blockState);
        }
    }

    @Unique
    private PaintingInfo getInfo() {
        SignBlockEntityAccessor sign = (SignBlockEntityAccessor) blockEntity;
        return front ? sign.signedPaintings$getFrontPaintingInfo() : sign.signedPaintings$getBackPaintingInfo();
    }

    @Unique
    private boolean isInfoCorrect() {
        PaintingInfo info = getInfo();
        return info != null && info.isReady();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isInfoCorrect() || UIHelper.isBackgroundEnabled()) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;getTextScale()Lorg/joml/Vector3f;"))
    private Vector3f modifyGetTextScale(AbstractSignEditScreen instance) {
        return new Vector3f(1.0f, 1.0f, 1.0f); // For some reason Signs had ugly 0.96
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void init(CallbackInfo ci) {
        doneButton = (ClickableWidget)this.children().get(0);

        UIHelper.init(front, this, (SignBlockEntityAccessor) blockEntity);
        ArrayList<ClickableWidget> uiButtons = UIHelper.getButtons();
        for (ClickableWidget widget : uiButtons) {
            addDrawableChild(widget);
            addSelectableChild(widget);
        }

        uploadButton = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".create_prompt"), button -> this.createPainting()).dimensions(this.width / 2 - 100, (this.height / 4 + 144)-25, 200, 20).build();
        addDrawableChild(uploadButton);
        addSelectableChild(uploadButton);

        BackgroundClick backgroundClick = new BackgroundClick(UIHelper.getInputSliders());
        addSelectableChild(backgroundClick);
        UIHelper.addButton(backgroundClick);

        SignedPaintingsClient.currentSignEdit.setSelectionManager(selectionManager);

        boolean correct = isInfoCorrect();
        signedPaintings$setVisibility(correct);
        String currentUrl = ((SignBlockEntityAccessor)blockEntity).signedPaintings$getSideInfo(front).getUrl();
        if (correct || currentUrl.isBlank() || currentUrl.equals("https://")) {
            uploadButton.visible = false;
        } else {
            url = currentUrl;
            updateUploadButton(true);
            url = null;
        }
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
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && slider.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "charTyped", cancellable = true)
    private void onCharType(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && slider.charTyped(chr, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @ModifyVariable(at = @At("STORE"), method = "renderSignText", ordinal = 0)
    private boolean stopTextCaret(boolean bl) {
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && selectionManager != null) {
                selectionManager.setSelectionEnd(selectionManager.getSelectionStart());
                return false;
            }
        }
        return bl;
    }

    @Override
    public void signedPaintings$clear(boolean setText) {
        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = "";
            this.text = this.text.withMessage(i, Text.literal(""));
        }
        if (setText) {
            this.blockEntity.setText(this.text, this.front);
        }
        this.currentRow = 0;
    }

    @Override
    public int signedPaintings$paste(String pasteString, int selectionStart, int selectionEnd, boolean setText) {
        int maxWidthPerLine = this.blockEntity.getMaxTextWidth();
        TextRenderer textRenderer = SignedPaintingsClient.client.textRenderer;

        String pasteURL = SignedPaintingsClient.imageManager.applyURLInferences(pasteString);

        if (ImageManager.isValid(pasteString) || pasteString.matches(".*([/:\\\\]).*\\|$")) {
            url = pasteURL;
            if (url.startsWith("https://images-ext-1.discordapp.net/external/")) {
                url = url.substring(url.substring(45).indexOf('/')+46).replaceFirst("/","://");
            }
            updateUploadButton(false);
        }

        String[] newMessages = new String[messages.length];
        System.arraycopy(messages, 0, newMessages, 0, messages.length);

        selectionStart = MathHelper.clamp(selectionStart, 0, newMessages[currentRow].length());
        selectionEnd = MathHelper.clamp(selectionEnd, 0, newMessages[currentRow].length());
        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        newMessages[currentRow] = newMessages[currentRow].substring(0, selectionStart) + pasteString + newMessages[currentRow].substring(selectionEnd);
        int currentWidth = textRenderer.getWidth(newMessages[currentRow]);
        int cursor = selectionStart + pasteString.length();

        if (currentWidth < maxWidthPerLine) {
            setCurrentRowMessage(newMessages[currentRow]);
            return cursor;
        }

        int cursorRow = currentRow;

        while (true) {
            String line = newMessages[currentRow];
            int index = SignedPaintingsClient.getMaxFittingIndex(line, maxWidthPerLine, textRenderer);
            newMessages[currentRow] = line.substring(0, index);
            if (currentRow == messages.length - 1 || line.length() <= index) {
                break;
            }
            if (currentRow == cursorRow && cursor > index) {
                cursorRow++;
                cursor -= index;
            }
            currentRow++;
            newMessages[currentRow] = line.substring(index) + newMessages[currentRow];
        }
        cursor = MathHelper.clamp(cursor, 0, newMessages[cursorRow].length());

        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = newMessages[i];
            this.text = this.text.withMessage(i, Text.literal(this.messages[i]));
        }

        if (setText) {
            this.blockEntity.setText(this.text, this.front);
        }

        currentRow = cursorRow;
        return cursor;
    }

    @Unique
    private void updateUploadButton(boolean isExisting) {
        int start = url.indexOf('/')+2;
        domain = url.substring(0, url.substring(start).indexOf('/')+start+1);
        boolean blocked = SignedPaintingsClient.imageManager.domainBlocked(domain);

        String key = blocked ? (isExisting ? ".allow" : ".create_allow") : ".create";
        uploadButton.setMessage(Text.translatable(SignedPaintingsClient.MODID + key));
        uploadButton.setTooltip(Tooltip.of(Text.translatable(SignedPaintingsClient.MODID + key+"_info", domain.substring(start, domain.length()-1), Text.translatable(SignedPaintingsClient.MODID + ".allow_disclaimer"))));
        uploadButton.visible = true;
    }

    @Unique
    private void createPainting() {
        SignedPaintingsClient.imageManager.allowDomain(domain);
        SignedPaintingsClient.imageManager.blockPromptedDomains.remove(domain);
        uploadButton.visible = false;

        if (url == null) return;

        signedPaintings$clear(false);
        int newSelection = signedPaintings$paste(SignByteMapper.INITIALIZER_STRING + SignByteMapper.encode(SignedPaintingsClient.imageManager.getShortestURLInference(url)), 0, 0, false);
        selectionManager.setSelection(newSelection, newSelection);
        ((SignBlockEntityAccessor) this.blockEntity).signedPaintings$getSideInfo(this.front).loadPainting(this.front, this.blockEntity, true);

        url = null;
    }

    @Override
    public void signedPaintings$setVisibility(boolean to) {
        for (ClickableWidget clickableWidget : UIHelper.getButtons()) {
            clickableWidget.visible = to;
        }

        doneButton.visible = !to;
    }

    @Override
    public void signedPaintings$initSliders(SignSideInfo info) {
        UIHelper.updateUI(info);
    }

    @Override
    public String signedPaintings$getText() {
        StringBuilder s = new StringBuilder();
        for (String message : messages) {
            s.append(message);
        }
        return s.toString();
    }
}
