package com.nettakrim.signed_paintings;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class InputSlider {
    public InputTextFieldWidget textFieldWidget;

    public InputSliderWidget sliderWidget;

    private float value;

    private static final Predicate<String> textPredicate = text -> StringUtils.countMatches(text, '.') <= 1 && text.replaceAll("[^0-9.]", "").length() == text.length();

    public InputSlider(int x, int y, int textWidth, int sliderWidth, int height, int elementSpacing, float minSlider, float maxSlider, float sliderStep, float startingValue) {
        textFieldWidget = createTextField(x, y, textWidth, height);
        textFieldWidget.setChangedListener(this::onTextChanged);
        textFieldWidget.setTextPredicate(textPredicate);

        sliderWidget = createSlider(x+textWidth+elementSpacing, y, sliderWidth, height, minSlider, maxSlider, sliderStep);
        sliderWidget.setChangedListener(this::onSliderChanged);

        setValue(startingValue);
    }

    private InputTextFieldWidget createTextField(int x, int y, int width, int height) {
        return new InputTextFieldWidget(SignedPaintingsClient.client.textRenderer, x, y, width, height, Text.literal("0"));
    }

    private InputSliderWidget createSlider(int x, int y, int width, int height, float min, float max, float step) {
        return new InputSliderWidget(x, y, width, height, Text.literal("test:"), min, max, step, 0.5f);
    }

    public boolean isActive() {
        return textFieldWidget.isActive();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return textFieldWidget.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return textFieldWidget.charTyped(chr, modifiers);
    }

    public void onTextChanged(String newValue) {
        try {
            value = Float.parseFloat(newValue);
            updateSlider();
        }
        catch (NumberFormatException ignored) {

        }
    }

    public void onSliderChanged(float newValue) {
        value = newValue;
        updateTextField();
    }

    public void setValue(float to) {
        value = to;
        updateTextField();
        updateSlider();
    }

    private void updateSlider() {
        sliderWidget.setValue(value);
    }

    private void updateTextField() {
        textFieldWidget.setChangedListener(null);
        textFieldWidget.setText(Float.toString(value));
        textFieldWidget.setChangedListener(this::onTextChanged);
    }

    public static class InputTextFieldWidget extends TextFieldWidget {
        public InputTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
            super(textRenderer, x, y, width, height, text);
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
        protected void updateMessage() {
            this.setMessage(Text.literal(SignedPaintingsClient.floatToStringDP((float)(min + (max-min) * value), 2)));
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
