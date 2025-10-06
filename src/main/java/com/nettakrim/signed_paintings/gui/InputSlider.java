package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InputSlider {
    public final InputTextFieldWidget textFieldWidget;

    public final InputSliderWidget sliderWidget;

    private float value;
    private final float minValue;
    private final float maxValue;

    private static final Predicate<String> textPredicate = text -> StringUtils.countMatches(text, '.') <= 1 && text.replaceAll("[^0-9.-]", "").length() == text.length();

    private Consumer<Float> onValueChanged;

    public InputSlider(int x, int y, int textWidth, int sliderWidth, int height, int elementSpacing, float minSlider, float maxSlider, float sliderStep, float startingValue, float minValue, float maxValue, Text text) {
        this.minValue = minValue;
        this.maxValue = maxValue;

        sliderWidget = createSlider(x, y, sliderWidth, height, text, minSlider, maxSlider, sliderStep);
        sliderWidget.setChangedListener(this::onSliderChanged);

        textFieldWidget = createTextField(x+sliderWidth+elementSpacing+1, y+1, textWidth-2, height-2);
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

    public boolean keyPressed(KeyInput input) {
        if (textFieldWidget.isActive()) {
            return textFieldWidget.keyPressed(input);
        } else if (sliderWidget.isFocused()) {
            return sliderWidget.keyPressed(input);
        }
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (textFieldWidget.isActive()) {
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
            value = MathHelper.clamp(newValue, minValue, maxValue);
            if (onValueChanged != null) onValueChanged.accept(value);
        }
    }

    public void setValue(float to) {
        value = MathHelper.clamp(to, minValue, maxValue);
        updateTextField();
        updateSlider();
    }

    private void updateSlider() {
        sliderWidget.setValue(value);
    }

    private void updateTextField() {
        textFieldWidget.setChangedListener(null);
        textFieldWidget.setText(Float.toString(value));
        textFieldWidget.setCursorToStart(false);
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
        public boolean keyPressed(KeyInput input) {
            if (input.getKeycode() == 257) {
                this.setFocused(false);
                return true;
            } else if (input.getKeycode() == 258) {
                return false;
            } else {
                return super.keyPressed(input);
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
        public boolean keyPressed(KeyInput input) {
            if (input.getKeycode() == 263 || input.getKeycode() == 262) {
                value = MathHelper.clamp(value + (input.getKeycode() == 263 ? -step : step)/(max-min), 0, 1);
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
            value = MathHelper.clamp(to, 0, 1);
            updateMessage();
        }
    }
}
