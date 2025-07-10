package com.nettakrim.signed_paintings.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ClientIDWidget extends TextFieldWidget {
    private Consumer<String> changed;

    private final String initial;

    public ClientIDWidget(TextRenderer textRenderer, Text text, int x, int y, int width, int height) {
        super(textRenderer, x, y, width, height, Text.literal("client id"));
        initial = text.getString();
        setText(initial);
        super.setChangedListener(this::onChange);
        setMaxLength(15);
    }

    @Override
    public void setChangedListener(Consumer<String> changedListener) {
        changed = changedListener;
    }

    @Override
    public void setCursor(int cursor, boolean shiftKeyPressed) {
        setSelectionStart(0);
        setSelectionEnd(getText().length());
    }

    private void onChange(String s) {
        if (s.matches("^[a-z0-9]{15}$")) {
            changed.accept(s);
            setText(initial);
        }
    }
}
