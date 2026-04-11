package com.nettakrim.signed_paintings.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class BackgroundClick extends AbstractWidget {
    private final InputSlider[] sliders;

    public BackgroundClick(InputSlider[] sliders, int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.sliders = sliders;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!(visible && isValidClickButton(click.buttonInfo()))) return false;
        for (InputSlider inputSlider : sliders) {
            if (inputSlider.isFocused()) return true;
        }
        return false;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {}
}
