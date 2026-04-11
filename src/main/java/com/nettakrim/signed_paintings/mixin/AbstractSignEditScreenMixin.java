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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    protected SignBlockEntity sign;

    @Final
    @Shadow
    private boolean isFrontText;

    @Shadow
    private int line;

    @Shadow
    private TextFieldHelper signField;

    @Unique
    private String url = null;

    @Unique
    private String domain = null;

    @Unique
    private StringWidget disclaimer;

    @Unique
    private AbstractWidget uploadButton;

    @Unique
    private AbstractWidget doneButton;

    protected AbstractSignEditScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract void setMessage(String message);

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean shouldRenderTitle(GuiGraphicsExtractor context, Font font, Component text, int x, int y, int color){
        return !isInfoCorrect();
    }

    @WrapOperation(method = "extractSign", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"))
    private Matrix3x2f translateForRender(Matrix3x2fStack instance, float x, float y, Operation<Matrix3x2f> original){
        if (isInfoCorrect()) {
            float offset = 0f;
            //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
            if (this.getClass().equals(SignEditScreen.class)) {
                offset = sign.getBlockState().getBlock() instanceof StandingSignBlock ? -16.0f : -4.0f;
            }
            // 97.5 is centered, but it looks a bit weird, deliberately offcentering it ends up looking better
            original.call(instance, 86f, 38.0f + offset);
            instance.scale(0.5f, 0.5f);
        } else {
            original.call(instance, x, y);
        }
        return instance;
    }

    @Unique
    private PaintingInfo getInfo() {
        SignBlockEntityAccessor signAccessor = (SignBlockEntityAccessor) sign;
        return isFrontText ? signAccessor.signedPaintings$getFrontPaintingInfo() : signAccessor.signedPaintings$getBackPaintingInfo();
    }

    @Unique
    private boolean isInfoCorrect() {
        PaintingInfo info = getInfo();
        return info != null && info.isReady();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isInfoCorrect() || UIHelper.isBackgroundEnabled()) {
            super.extractBackground(context, mouseX, mouseY, delta);
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void onInit(CallbackInfo ci) {
        if (!this.children().isEmpty()) {
            doneButton = (AbstractWidget) this.children().getFirst();
        }

        UIHelper.init(isFrontText, this, (SignBlockEntityAccessor) sign);
        ArrayList<AbstractWidget> uiButtons = UIHelper.getButtons();
        for (AbstractWidget widget : uiButtons) {
            addRenderableWidget(widget);
            addWidget(widget);
        }

        int y = FabricLoader.getInstance().isModLoaded("stendhal") ? 40 : (this.height / 4 + 144);

        disclaimer = new StringWidget(0, y-43, this.width, 25, Component.empty(), font);
        disclaimer.visible = false;
        addRenderableWidget(disclaimer);

        uploadButton = Button.builder(Component.translatable(SignedPaintingsClient.MODID + ".create_prompt"), button -> this.createPainting()).bounds(this.width / 2 - 100, y-25, 200, 20).build();
        addRenderableWidget(uploadButton);
        addWidget(uploadButton);

        BackgroundClick backgroundClick = new BackgroundClick(UIHelper.getInputSliders(), width, height);
        addWidget(backgroundClick);
        UIHelper.addBackground(backgroundClick);

        SignedPaintingsClient.currentSignEdit.setSelectionManager(signField);

        boolean correct = isInfoCorrect();
        signedPaintings$setVisibility(correct);
        SignSideInfo sideInfo = ((SignBlockEntityAccessor)sign).signedPaintings$getSideInfo(isFrontText);
        String currentUrl = sideInfo.getUrl();
        if (correct || currentUrl.isBlank() || currentUrl.equals("https://")) {
            uploadButton.visible = false;
        } else {
            url = currentUrl;
            updateUploadButton(true);
            url = null;
        }

        if (correct) {
            UIHelper.updateUI(sideInfo);
        }
    }


    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V")
    private void onScreenOpen(SignBlockEntity blockEntity, boolean front, boolean filtered, Component title, CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = new SignEditingInfo(blockEntity, this);
    }

    @Inject(at = @At("TAIL"), method = "onDone")
    private void onScreenClose(CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = null;
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    private void onKeyPress(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && slider.keyPressed(input)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "charTyped", cancellable = true)
    private void onCharType(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && slider.charTyped(input)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @ModifyVariable(at = @At("STORE"), method = "extractSignText", name = "showCursor")
    private boolean stopTextCaret(boolean showCursor) {
        for (InputSlider slider : UIHelper.getInputSliders()) {
            if (slider != null && slider.isFocused() && signField != null) {
                signField.setSelectionPos(signField.getCursorPos());
                return false;
            }
        }
        return showCursor;
    }

    @Override
    public void signedPaintings$clear(boolean setText) {
        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = "";
            this.text = this.text.setMessage(i, Component.literal(""));
        }
        if (setText) {
            this.sign.setText(this.text, this.isFrontText);
        }
        this.line = 0;
    }

    @Override
    public int signedPaintings$paste(String pasteString, int selectionStart, int selectionEnd, boolean setText) {
        int maxWidthPerLine = this.sign.getMaxTextLineWidth();
        Font textRenderer = SignedPaintingsClient.client.font;

        String pasteURL = SignedPaintingsClient.imageManager.applyURLInferences(pasteString);

        if (ImageManager.isValid(pasteString) || pasteString.matches(".*([/:\\\\]).*\\|$")) {
            url = pasteURL;
            if (url.startsWith("https://images-ext-1.discordapp.net/external/")) {
                url = URLDecoder.decode(url.substring(url.substring(45).indexOf('/')+46).replaceFirst("/","://"), StandardCharsets.UTF_8);
            }
            updateUploadButton(false);
        }

        String[] newMessages = new String[messages.length];
        System.arraycopy(messages, 0, newMessages, 0, messages.length);

        selectionStart = Mth.clamp(selectionStart, 0, newMessages[line].length());
        selectionEnd = Mth.clamp(selectionEnd, 0, newMessages[line].length());
        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        newMessages[line] = newMessages[line].substring(0, selectionStart) + pasteString + newMessages[line].substring(selectionEnd);
        int currentWidth = textRenderer.width(newMessages[line]);
        int cursor = selectionStart + pasteString.length();

        if (currentWidth < maxWidthPerLine) {
            setMessage(newMessages[line]);
            return cursor;
        }

        int cursorRow = line;

        while (true) {
            String lineText = newMessages[line];
            int index = SignedPaintingsClient.getMaxFittingIndex(lineText, maxWidthPerLine, textRenderer);
            newMessages[line] = lineText.substring(0, index);
            if (line == messages.length - 1 || lineText.length() <= index) {
                break;
            }
            if (line == cursorRow && cursor > index) {
                cursorRow++;
                cursor -= index;
            }
            line++;
            newMessages[line] = lineText.substring(index) + newMessages[line];
        }
        cursor = Mth.clamp(cursor, 0, newMessages[cursorRow].length());

        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = newMessages[i];
            this.text = this.text.setMessage(i, Component.literal(this.messages[i]));
        }

        if (setText) {
            this.sign.setText(this.text, this.isFrontText);
        }

        line = cursorRow;
        return cursor;
    }

    @Unique
    private void updateUploadButton(boolean isExisting) {
        // stendhal compat
        if (uploadButton == null) {
            onInit(null);
        }

        int start = url.indexOf('/')+2;
        domain = url.substring(0, url.substring(start).indexOf('/')+start+1);
        boolean blocked = SignedPaintingsClient.imageManager.domainBlocked(domain);

        String key = blocked ? (isExisting ? ".trust" : ".create_trust") : ".create";
        uploadButton.setMessage(Component.translatable(SignedPaintingsClient.MODID + key));
        uploadButton.setTooltip(Tooltip.create(Component.translatable(SignedPaintingsClient.MODID + key+"_info", domain.substring(start, domain.length()-1), Component.translatable(SignedPaintingsClient.MODID + ".trust_disclaimer"))));
        uploadButton.visible = true;

        if (url.startsWith("https://media.discordapp.net")) {
            url = url.replace("format=webp", "format=png");
            activateDisclaimer(Component.translatable(SignedPaintingsClient.MODID+".disclaimer.discord"));
            return;
        }

        URI uri = URI.create(url);
        String path = uri.getPath();
        int i = Math.max(path.lastIndexOf('.'), path.lastIndexOf('@'));

        if (i == -1) {
            activateDisclaimer(Component.translatable(SignedPaintingsClient.MODID + ".disclaimer.image_address"));
            return;
        }

        String format = path.substring(i+1);

        for (String supported : ImageIO.getReaderFormatNames()) {
            if (supported.equals(format)) {
                return;
            }
        }

        activateDisclaimer(Component.translatable(SignedPaintingsClient.MODID + ".disclaimer.format", format));
    }

    @Unique
    private void activateDisclaimer(Component text) {
        disclaimer.setMessage(text);
        disclaimer.visible = true;
        disclaimer.setX((width - disclaimer.getWidth()) / 2);
    }

    @Unique
    private void createPainting() {
        SignedPaintingsClient.imageManager.trustDomain(domain);
        SignedPaintingsClient.imageManager.blockPromptedDomains.remove(domain);
        uploadButton.visible = false;
        disclaimer.visible = false;

        if (url == null) return;

        signedPaintings$clear(false);
        int newSelection = signedPaintings$paste(SignByteMapper.INITIALIZER_STRING + SignByteMapper.encode(SignedPaintingsClient.imageManager.getShortestURLInference(url)), 0, 0, false);
        signField.setSelectionRange(newSelection, newSelection);

        SignSideInfo info = ((SignBlockEntityAccessor) this.sign).signedPaintings$getSideInfo(this.isFrontText);
        info.loadPainting(this.isFrontText, this.sign, true);

        url = null;
    }

    @Override
    public void signedPaintings$setVisibility(boolean to) {
        for (AbstractWidget clickableWidget : UIHelper.getButtons()) {
            clickableWidget.visible = to;
        }

        if (doneButton != null) {
            doneButton.visible = !to;
        } else {
            // stendhal compat
            if (uploadButton == null) {
                onInit(null);
                return;
            }

            // litematica sets text before the edit screen appears when touching a sign in a schematic
            // doing nothing here causes everything to break moments later
            // just force closing the screen stops this
            // TODO: fix this propery?
            Minecraft.getInstance().schedule(() -> Minecraft.getInstance().setScreen(null));
            signField = new TextFieldHelper(() -> "", (s) -> {}, () -> "", (s) -> {}, (s) -> true);
            onInit(null);
        }
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

    @Override
    public int signedPaintings$internalRenderState() {
        if (!isInfoCorrect()) {
            return 0;
        }
        return sign.getBlockState().getBlock() instanceof StandingSignBlock ? -16 : -4;
    }
}
