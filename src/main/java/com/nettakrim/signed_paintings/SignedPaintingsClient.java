package com.nettakrim.signed_paintings;

import com.nettakrim.signed_paintings.commands.SignedPaintingsCommands;
import com.nettakrim.signed_paintings.gui.SignEditingInfo;
import com.nettakrim.signed_paintings.rendering.PaintingRenderer;
import com.nettakrim.signed_paintings.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static MinecraftClient client;

	public static ImageManager imageManager;
	public static PaintingRenderer paintingRenderer;

	public static SignEditingInfo currentSignEdit;

	public static final TextColor textColor = TextColor.fromRgb(0xAAAAAA);
	public static final TextColor nameTextColor = TextColor.fromRgb(0x4BCCA3);

	public static boolean renderSigns;
	public static boolean renderBanners;
	public static boolean renderShields;
	public static boolean reduceCulling;

	public static boolean loggingEnabled = false;

	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();

		imageManager = new ImageManager();

		imageManager.registerURLAlias(new NormalAlias("https://i.imgur.com/", new String[]{"i.imgur.com/","imgur.com/","imgur:"}, ".png"));
		imageManager.registerURLAlias(new NormalAlias("https://iili.io/", new String[]{"freeimage.host/i/", "iili:"}, ".png"));
		imageManager.registerAllowedDomain("https://i.imgur.com/");
		imageManager.registerAllowedDomain("https://iili.io/");
		imageManager.registerAllowedDomain("https://i.ibb.co/");

		paintingRenderer = new PaintingRenderer();
		renderSigns = true;
		renderBanners = true;
		renderShields = true;
		reduceCulling = false;

		WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> {
			MatrixStack matrices = context.matrixStack();
			VertexConsumerProvider vertexConsumers = context.consumers();

			SignedPaintingsClient.paintingRenderer.renderTranslucentQueue(matrices, vertexConsumers);
		});

		SignedPaintingsCommands.initialize();
	}

	public static String combineSignText(SignText text) {
		Text[] layers = text.getMessages(false);
		if (layers == null) return "";
		StringBuilder combined = new StringBuilder();
		for (Text line : layers) {
			if (line != null) combined.append(line.getString());
		}
		return combined.toString();
	}

	public static int getMaxFittingIndex(String reference, int budgetWidth, TextRenderer textRenderer) {
		//the string->width function can be considered as a sorted array where array[N] is the width of the first N characters of our string
		//this means it can be binary searched, resulting in an index representing the most first N characters that are at or below the budget width

		//the code
		//  index = reference.length();
		//  while (textRenderer.getWidth(reference.substring(0, index)) > budgetWidth) index--;
		//  return index;
		//should function identically

		//limit to 80 characters, since paper(?) additionally limits character count
		int charLength = reference.length();
		if (charLength > 80 && !MinecraftClient.getInstance().isInSingleplayer()) {
			charLength = 80;
		}

		int low = 0;
		int high = reference.codePointCount(0, charLength);
		int index = Integer.MAX_VALUE;

		while (low <= high) {
			int mid = low + ((high - low) / 2);
			int currentWidth = textRenderer.getWidth(codePointSubstring(reference, mid));
			if (currentWidth < budgetWidth) {
				low = mid + 1;
			} else if (currentWidth > budgetWidth) {
				high = mid - 1;
			} else {
				return reference.offsetByCodePoints(0, mid);
			}
			index = mid;
		}

		//length was not directly achievable, so use the next smallest length instead
		if (textRenderer.getWidth(codePointSubstring(reference, index)) > budgetWidth) index--;
		return reference.offsetByCodePoints(0, index);
	}

	public static String codePointSubstring(String s, int end) {
		int a = s.offsetByCodePoints(0, 0);
		return s.substring(a, s.offsetByCodePoints(a, end));
	}

	public static String floatToStringDP(float d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(d);
		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		String s1 =  bd.toString();
		String s2 = Float.toString(d);
		return s1.length() < s2.length() ? s1 : s2;
	}

	public static float roundFloatTo3DP(float value) {
		//various conversions try get rid of awkwardly long numbers like 1.499 or 3.002
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(3, RoundingMode.HALF_UP);
		BigDecimal bd2 = bd.setScale(2, RoundingMode.HALF_UP);
		double difference = Math.abs(bd.subtract(bd2).doubleValue());
		String s = bd.toString();
		if ((difference < 0.0011 || s.contains("00")) && !s.endsWith(".667") && !s.endsWith(".334")) {
			return bd2.floatValue();
		} else {
			return bd.floatValue();
		}
	}

	public static void sayStyled(String key, Style style, Object... args) {
		Text text = Text.translatable(MODID+".say").setStyle(style.withColor(nameTextColor)).append(Text.translatable(MODID+"."+key, args).setStyle(Style.EMPTY.withColor(textColor)));
		sayRaw(text);
	}

	public static void sayTranslated(String key, Object... args) {
		sayText(Text.translatable(MODID+"."+key, args));
	}

	public static void sayText(MutableText text) {
		Text newText = Text.translatable(MODID+".say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		sayRaw(newText);
	}

	public static void longSay(MutableText text) {
		Text newText = Text.translatable(MODID+".long_say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		sayRaw(newText);
	}

	public static void sayRaw(Text text) {
		if (client.player == null) return;
		client.player.sendMessage(text, false);
	}

	public static String getScreenshotDirectory() {
		return SignedPaintingsClient.client.runDirectory+"\\"+ ScreenshotRecorder.SCREENSHOTS_DIRECTORY+"\\";
	}

	public static void info(String s, boolean force) {
		if (loggingEnabled || force) LOGGER.info(s);
	}
}