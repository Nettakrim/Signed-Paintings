package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class InputSlider {
    public final InputTextFieldWidget textFieldWidget;

    public final InputSliderWidget sliderWidget;

    private float value;

    private static final Predicate<String> textPredicate = text -> StringUtils.countMatches(text, '.') <= 1 && text.replaceAll("[^0-9.]", "").length() == text.length();

    private Consumer<Float> onValueChanged;

    public InputSlider(int x, int y, int textWidth, int sliderWidth, int height, int elementSpacing, float minSlider, float maxSlider, float sliderStep, float startingValue, Text text) {
        sliderWidget = createSlider(x, y, sliderWidth, height, text, minSlider, maxSlider, sliderStep);
        sliderWidget.setChangedListener(this::onSliderChanged);

        textFieldWidget = createTextField(x+textWidth+elementSpacing+1, y+1, textWidth-2, height-2);
        textFieldWidget.setChangedListener(this::onTextChanged);
        textFieldWidget.setTextPredicate(textPredicate);

        setValue(startingValue);
    }

    private InputTextFieldWidget createTextField(int x, int y, int width, int height) {
        return new InputTextFieldWidget(SignedPaintingsClient.client.textRenderer, x, y, width, height, Text.literal("0"));
    }

    private InputSliderWidget createSlider(int x, int y, int width, int height, Text text, float min, float max, float step) {
        return new InputSliderWidget(x, y, width, height, text, min, max, step, 0.5f);
    }

    public void setOnValueChanged(Consumer<Float> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    public boolean isFocused() {
        return textFieldWidget.isFocused() || sliderWidget.isFocused();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textFieldWidget.isActive()) {
            return textFieldWidget.keyPressed(keyCode, scanCode, modifiers);
        } else if (sliderWidget.isFocused()) {
            return sliderWidget.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (textFieldWidget.isActive()) {
            return textFieldWidget.charTyped(chr, modifiers);
        } else if (sliderWidget.isFocused()) {
            return sliderWidget.charTyped(chr, modifiers);
        }
        return false;
    }

    public void onTextChanged(String newValue) {
        try {
            value = MathHelper.clamp(Float.parseFloat(newValue), 1f/32f, 128f);
            if (onValueChanged != null) onValueChanged.accept(value);
            updateSlider();
        }
        catch (NumberFormatException ignored) {

        }
    }

    public void onSliderChanged(float newValue) {
        value = MathHelper.clamp(newValue, 1f/32f, 128f);
        if (onValueChanged != null) onValueChanged.accept(value);
        updateTextField();
    }

    public void setValue(float to) {
        value = MathHelper.clamp(to, 1f/32f, 128f);
        updateTextField();
        updateSlider();
    }

    private void updateSlider() {
        sliderWidget.setValue(value);
    }

    private void updateTextField() {
        textFieldWidget.setChangedListener(null);
        textFieldWidget.setText(Float.toString(value));
        textFieldWidget.setCursorToStart();
        textFieldWidget.setChangedListener(this::onTextChanged);
    }

    public float getValue() {
        return value;
    }

    public static class InputTextFieldWidget extends TextFieldWidget {
        public InputTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
            super(textRenderer, x, y, width, height, text);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 257) {
                this.setFocused(false);
                return true;
            } else if (keyCode == 258) {
                return false;
            } else {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public static class InputSliderWidget extends SliderWidget {
        private final float min;
        private final float max;
        private final float step;
        private Consumer<Float> onChange;

        public InputSliderWidget(int x, int y, int width, int height, Text text, float min, float max, float step, double value) {
            super(x, y, width, height, text, value);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public void setChangedListener(Consumer<Float> onChange) {
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {}

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 263 || keyCode == 262) {
                value = MathHelper.clamp(value + (keyCode == 263 ? -step : step)/(max-min), 0, 1);
                applyValue();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        protected void applyValue() {
            float round = (max-min)/step;
            value = Math.round(value*round)/round;
            onChange.accept((float)(min + (max-min) * value));
        }

        public void setValue(float to) {
            //to = Math.round(to/step)*step;
            to = (to - min)/(max - min);
            value = MathHelper.clamp(to, 0, 1);
            updateMessage();
        }
    }
}
