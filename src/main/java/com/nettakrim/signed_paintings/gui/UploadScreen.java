package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.UploadManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.function.Consumer;

public class UploadScreen extends Screen {
    private final Screen previous;
    private final String url;
    private final Consumer<String> onSuccess;

    private ClickableWidget globalButton;

    private final ArrayList<ClickableWidget> main = new ArrayList<>();
    private final ArrayList<ClickableWidget> guide = new ArrayList<>();

    private final float[] ratios = new float[] {
            572f/463f,
            910f/736f,
            387f/721f
    };

    private boolean isGuide = false;

    public UploadScreen(Screen previous, String url, Consumer<String> onSuccess) {
        super(Text.empty());

        this.previous = previous;
        this.url = url;
        this.onSuccess = onSuccess;
    }

    @Override
    protected void init() {
        main.clear();
        guide.clear();

        globalButton = ButtonWidget.builder(Text.translatable("signed_paintings.upload_shared"), this::upload).dimensions(this.width / 2 - 100, 20, 200, 20).build();
        main.add(addDrawableChild(globalButton));
        main.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable("signed_paintings.key_prompt"), (b) -> switchMode(true)).dimensions(this.width / 2 - 100, 50, 200, 20).build()));
        main.add(this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, (b) -> close()).dimensions(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()));


        float margin = width/27f;
        float f = (width - margin)/3f;
        float edge = margin/4f;
        int width = Math.round(f - margin + edge*2);

        guide.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable("signed_paintings.key_sign_in"), (b) -> openURL("https://imgur.com/")).dimensions(
                Math.round(margin-edge),       10, width, 20).build())
        );
        guide.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable("signed_paintings.key_register"), (b) -> openURL("https://api.imgur.com/oauth2/addclient")).dimensions(
                Math.round(f + margin-edge),   10, width, 20).build())
        );
        TextFieldWidget textFieldWidget = this.addDrawableChild(new ClientIDWidget(MinecraftClient.getInstance().textRenderer, Text.translatable("signed_paintings.key_paste"),
                Math.round(f*2 + margin-edge), 10, width, 20)
        );
        guide.add(textFieldWidget);
        textFieldWidget.setChangedListener(this::keySet);

        guide.add(this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, (b) -> switchMode(false)).dimensions(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()));


        switchMode(isGuide);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(previous);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isGuide) {
            float margin = width/27f;
            float f = (width - margin)/3f;
            for (int i = 0; i < 3; i++) {
                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.of(SignedPaintingsClient.MODID, "guide"+(i+1)), Math.round(f*i + margin), 40, Math.round(f-margin), Math.round((f-margin)*ratios[i]), -1);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void upload(ButtonWidget buttonWidget) {
        SignedPaintingsClient.uploadManager.uploadUrlToImgur(url, this::uploadFinished);
    }

    private void uploadFinished(String link) {
        if (link == null) {
            if (UploadManager.lastUploadRateLimited) {
                globalButton.setMessage(Text.literal("ratelimited"));
            } else {
                globalButton.setMessage(Text.literal("upload failed"));
            }
            return;
        }

        onSuccess.accept(link);
        close();
    }

    private void switchMode(boolean to) {
        for (ClickableWidget widget : main) {
            widget.visible = !to;
        }

        for (ClickableWidget widget : guide) {
            widget.visible = to;
        }

        isGuide = to;
    }

    private void keySet(String key) {
        switchMode(false);
    }

    private void openURL(String url) {
        Util.getOperatingSystem().open(url);
    }
}
