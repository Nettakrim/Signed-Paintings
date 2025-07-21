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
    private final Consumer<String> onUpload;

    private final String domain;
    private final Text info;

    private ClickableWidget globalButton;

    private final ArrayList<ClickableWidget> main = new ArrayList<>();
    private final ArrayList<ClickableWidget> guide = new ArrayList<>();

    private final float[] ratios = new float[] {
            572f/463f,
            910f/736f,
            387f/721f
    };

    private boolean isGuide = false;

    public UploadScreen(Screen previous, String url, Consumer<String> onUpload) {
        super(Text.empty());

        this.previous = previous;
        this.url = url;
        this.onUpload = onUpload;

        int start = url.indexOf('/')+2;
        domain = url.substring(0, url.substring(start).indexOf('/')+start+1);

        Text separator = Text.literal("\n\n");
        info = Text.empty()
                .append(Text.translatable(SignedPaintingsClient.MODID + ".info_upload")).append(separator)
                .append(Text.translatable(SignedPaintingsClient.MODID + ".info_allow_domain")).append(separator)
                .append(Text.translatable(SignedPaintingsClient.MODID + ".info_allow_all")).append(separator)
                .append(Text.translatable(SignedPaintingsClient.MODID + ".info_domain", domain).append(separator))
                .append(Text.translatable(SignedPaintingsClient.MODID + ".info_url", ".../"+url.substring(domain.length())));
    }

    @Override
    protected void init() {
        main.clear();
        guide.clear();

        int x = width/2 + 5;
        int w = width/2 - 10;

        globalButton = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".upload_shared"), this::upload).dimensions(x, 5, w, 20).build();
        main.add(addDrawableChild(globalButton));

        boolean hasAll = hasDomain("https://");

        ButtonWidget domainButton = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".allow_domain_" + (hasDomain(domain) ? "off": "on")), (b) -> toggleDomain(domain)).dimensions(x, 35, w, 20).build();
        ButtonWidget allButton = ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".allow_all_" + (hasAll ? "off": "on")), (b) -> toggleDomain("https://")).dimensions(x, 65, w, 20).build();
        main.add(this.addDrawableChild(domainButton));
        main.add(this.addDrawableChild(allButton));

        domainButton.active = !hasAll;

        // disabled, but partially implemented in case imgur fixes itself
        //main.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable("signed_paintings.key_prompt"), (b) -> switchMode(true)).dimensions(this.width / 2 - 100, 50, 200, 20).build()));

        main.add(this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, (b) -> close()).dimensions(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()));

        float margin = width/27f;
        float f = (width - margin)/3f;
        float edge = margin/4f;
        int width = Math.round(f - margin + edge*2);

        guide.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".key_sign_in"), (b) -> openURL("https://imgur.com/")).dimensions(
                Math.round(margin-edge),       10, width, 20).build())
        );
        guide.add(this.addDrawableChild(ButtonWidget.builder(Text.translatable(SignedPaintingsClient.MODID + ".key_register"), (b) -> openURL("https://api.imgur.com/oauth2/addclient")).dimensions(
                Math.round(f + margin-edge),   10, width, 20).build())
        );
        TextFieldWidget textFieldWidget = this.addDrawableChild(new ClientIDWidget(MinecraftClient.getInstance().textRenderer, Text.translatable(SignedPaintingsClient.MODID + ".key_paste"),
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
        } else {
            context.drawWrappedText(MinecraftClient.getInstance().textRenderer, info, 5, 5, width/2 - 10, -1, true);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            if (mouseX < width/2f - 5) {
                openURL(url);
                return true;
            }
            return false;
        }
        return true;
    }

    private void upload(ButtonWidget buttonWidget) {
        SignedPaintingsClient.uploadManager.uploadUrlToImgur(url, this::uploadFinished);
    }

    private void uploadFinished(String link) {
        if (link == null) {
            // TODO: translation
            if (UploadManager.lastUploadRateLimited) {
                globalButton.setMessage(Text.literal("ratelimited"));
            } else {
                globalButton.setMessage(Text.literal("upload failed"));
            }
        } else {
            close();
        }

        onUpload.accept(link);
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

    private void toggleDomain(String domain) {
        if (hasDomain(domain)) {
            SignedPaintingsClient.imageManager.removeAllowedDomain(domain);
        } else {
            SignedPaintingsClient.imageManager.registerAllowedDomain(domain);
            onUpload.accept(url);
        }
        SignedPaintingsClient.imageManager.reloadDomain(domain);
        close();
    }

    private boolean hasDomain(String domain) {
        return SignedPaintingsClient.imageManager.allowedDomains.contains(domain);
    }
}
