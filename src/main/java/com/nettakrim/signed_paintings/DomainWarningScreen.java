package com.nettakrim.signed_paintings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class DomainWarningScreen extends Screen {
    private final String domain;
    private final Consumer<String> confirm;

    private MultilineTextWidget text;

    public DomainWarningScreen(String domain, Consumer<String> confirm) {
        super(Text.empty());

        this.domain = domain;
        this.confirm = confirm;
    }

    @Override
    protected void init() {
        text = addDrawableChild(new MultilineTextWidget(0, height/6, Text.translatable(SignedPaintingsClient.MODID+".domain.warning_full"), textRenderer).setCentered(true).setMaxWidth(width));
        addDrawableChild(new ButtonWidget.Builder(Text.translatable(SignedPaintingsClient.MODID+".domain.warning_confirm"), this::confirm).dimensions(width/2 - 100, height/2, 200, 20).build());
        addDrawableChild(new ButtonWidget.Builder(Text.translatable(SignedPaintingsClient.MODID+".domain.warning_close"), this::close).dimensions(width/2 - 100, height/2 + 25, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        text.setPosition((width - text.getWidth())/2, height/6);

        super.render(context, mouseX, mouseY, delta);
    }

    public void close(ButtonWidget buttonWidget) {
        close();
    }

    public void confirm(ButtonWidget buttonWidget) {
        confirm.accept(domain);
        close();
    }
}
