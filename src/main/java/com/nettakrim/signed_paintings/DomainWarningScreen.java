package com.nettakrim.signed_paintings;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DomainWarningScreen extends Screen {
    private final String domain;
    private final Consumer<String> confirm;

    private MultiLineTextWidget text;

    public DomainWarningScreen(String domain, Consumer<String> confirm) {
        super(Component.empty());

        this.domain = domain;
        this.confirm = confirm;
    }

    @Override
    protected void init() {
        text = addRenderableWidget(new MultiLineTextWidget(0, height/6, Component.translatable(SignedPaintingsClient.MODID+".domain.warning_full"), font).setCentered(true).setMaxWidth(width));
        addRenderableWidget(new Button.Builder(Component.translatable(SignedPaintingsClient.MODID+".domain.warning_confirm"), this::confirm).bounds(width/2 - 100, height/2, 200, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable(SignedPaintingsClient.MODID+".domain.warning_close"), this::close).bounds(width/2 - 100, height/2 + 25, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        text.setPosition((width - text.getWidth())/2, height/6);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    public void close(Button buttonWidget) {
        onClose();
    }

    public void confirm(Button buttonWidget) {
        confirm.accept(domain);
        onClose();
    }
}
