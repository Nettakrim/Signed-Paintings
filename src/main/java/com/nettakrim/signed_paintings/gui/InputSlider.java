package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.SignedPaintingsClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class InputSlider {
    public final InputTextFieldWidget textFieldWidget;

    public final InputSliderWidget sliderWidget;

    private float value;
    private final float minValue;
    private final float maxValue;

    private Consumer<Float> onValueChanged;

    public InputSlider(int x, int y, int textWidth, int sliderWidth, int height, int elementSpacing, float minSlider, float maxSlider, float sliderStep, float startingValue, float minValue, float maxValue, Component text) {
        this.minValue = minValue;
        this.maxValue = maxValue;

        sliderWidget = createSlider(x, y, sliderWidth, height, text, minSlider, maxSlider, sliderStep);
        sliderWidget.setChangedListener(this::onSliderChanged);

        textFieldWidget = createTextField(x+sliderWidth+elementSpacing+1, y+1, textWidth-2, height-2);
        textFieldWidget.setResponder(this::onTextChanged);

        setValue(startingValue);
    }

    private InputTextFieldWidget createTextField(int x, int y, int width, int height) {
        return new InputTextFieldWidget(SignedPaintingsClient.client.font, x, y, width, height, Component.literal("0"));
    }

    private InputSliderWidget createSlider(int x, int y, int width, int height, Component text, float min, float max, float step) {
        return new InputSliderWidget(x, y, width, height, text, min, max, step, 0.5f);
    }

    public void setOnValueChanged(Consumer<Float> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    public boolean isFocused() {
        return textFieldWidget.isFocused() || sliderWidget.isFocused();
    }

    public boolean keyPressed(KeyEvent input) {
        if (textFieldWidget.canConsumeInput()) {
            return textFieldWidget.keyPressed(input);
        } else if (sliderWidget.isFocused()) {
            return sliderWidget.keyPressed(input);
        }
        return false;
    }

    public boolean charTyped(CharacterEvent input) {
        if (textFieldWidget.canConsumeInput()) {
            return textFieldWidget.charTyped(input);
        } else if (sliderWidget.isFocused()) {
            return sliderWidget.charTyped(input);
        }
        return false;
    }

    public void onTextChanged(String newValue) {
        try {
            onChange(Float.parseFloat(newValue));
            updateSlider();
        }
        catch (NumberFormatException ignored) {

        }
    }

    public void onSliderChanged(float newValue) {
        onChange(newValue);
        updateTextField();
    }

    private void onChange(float newValue) {
        if (Float.isFinite(newValue)) {
            value = Mth.clamp(newValue, minValue, maxValue);
            if (onValueChanged != null) onValueChanged.accept(value);
        }
    }

    public void setValue(float to) {
        value = Mth.clamp(to, minValue, maxValue);
        updateTextField();
        updateSlider();
    }

    private void updateSlider() {
        sliderWidget.setValue(value);
    }

    private void updateTextField() {
        textFieldWidget.setResponder((s) -> {});
        textFieldWidget.setValue(Float.toString(value));
        textFieldWidget.moveCursorToStart(false);
        textFieldWidget.setResponder(this::onTextChanged);
    }

    public float getValue() {
        return value;
    }

    public static class InputTextFieldWidget extends EditBox {
        public InputTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text) {
            super(textRenderer, x, y, width, height, text);
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (input.input() == 257) {
                this.setFocused(false);
                return true;
            } else if (input.input() == 258) {
                return false;
            } else {
                return super.keyPressed(input);
            }
        }
    }

    public static class InputSliderWidget extends AbstractSliderButton {
        private final float min;
        private final float max;
        private final float step;
        private Consumer<Float> onChange;

        public InputSliderWidget(int x, int y, int width, int height, Component text, float min, float max, float step, double value) {
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
        public boolean keyPressed(KeyEvent input) {
            if (input.input() == 263 || input.input() == 262) {
                value = Mth.clamp(value + (input.input() == 263 ? -step : step)/(max-min), 0, 1);
                applyValue();
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        protected void applyValue() {
            float round = (max-min)/step;
            value = Math.round(value*round)/round;
            float result = (float)(min + (max-min) * value);
            if (Double.isFinite(value)) {
                BigDecimal bd = new BigDecimal(result);
                onChange.accept(bd.setScale(3, RoundingMode.HALF_UP).floatValue());
            }
        }

        public void setValue(float to) {
            to = (to - min)/(max - min);
            value = Mth.clamp(to, 0, 1);
            updateMessage();
        }
    }
}
