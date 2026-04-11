package com.nettakrim.signed_paintings;

import com.nettakrim.signed_paintings.commands.SignedPaintingsCommands;
import com.nettakrim.signed_paintings.gui.SignEditingInfo;
import com.nettakrim.signed_paintings.rendering.PaintingRenderer;
import com.nettakrim.signed_paintings.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.block.entity.SignText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static Minecraft client;

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

	private static final ArrayList<Component> sayBuffer = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		client = Minecraft.getInstance();

		imageManager = new ImageManager();

		paintingRenderer = new PaintingRenderer();
		renderSigns = true;
		renderBanners = true;
		renderShields = true;
		reduceCulling = false;

		ClientTickEvents.START_CLIENT_TICK.register((context) -> {
			if (!sayBuffer.isEmpty()) {
				int size = sayBuffer.size();

				for (int i = 0; i < size; i++) {
					sayRaw(sayBuffer.removeFirst());
				}
			}

			imageManager.onTick();
		});

		SignedPaintingsCommands.initialize();
	}

	public static String combineSignText(SignText text) {
		Component[] layers = text.getMessages(false);
		if (layers == null) return "";
		StringBuilder combined = new StringBuilder();
		for (Component line : layers) {
			if (line != null) combined.append(line.getString());
		}
		return combined.toString();
	}

	public static int getMaxFittingIndex(String reference, int budgetWidth, Font textRenderer) {
		//the string->width function can be considered as a sorted array where array[N] is the width of the first N characters of our string
		//this means it can be binary searched, resulting in an index representing the most first N characters that are at or below the budget width

		//the code
		//  index = reference.length();
		//  while (textRenderer.getWidth(reference.substring(0, index)) > budgetWidth) index--;
		//  return index;
		//should function identically

		//limit to 80 characters, since paper(?) additionally limits character count
		int charLength = reference.length();
		if (charLength > 80 && !Minecraft.getInstance().isLocalServer()) {
			charLength = 80;
		}

		int low = 0;
		int high = reference.codePointCount(0, charLength);
		int index = Integer.MAX_VALUE;

		while (low <= high) {
			int mid = low + ((high - low) / 2);
			int currentWidth = textRenderer.width(codePointSubstring(reference, mid));
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
		if (textRenderer.width(codePointSubstring(reference, index)) > budgetWidth) index--;
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
		Component text = Component.translatable(MODID+".say").setStyle(style.withColor(nameTextColor)).append(Component.translatable(MODID+"."+key, args).setStyle(Style.EMPTY.withColor(textColor)));
		sayRaw(text);
	}

	public static void sayTranslated(String key, Object... args) {
		sayText(Component.translatable(MODID+"."+key, args));
	}

	public static void sayText(MutableComponent text) {
		Component newText = Component.translatable(MODID+".say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		sayRaw(newText);
	}

	public static void longSay(MutableComponent text) {
		Component newText = Component.translatable(MODID+".long_say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		sayRaw(newText);
	}

	public static void sayRaw(Component text) {
		if (client.player == null) {
			sayBuffer.add(text);
			return;
		}
		client.player.sendSystemMessage(text);
	}

	public static String getScreenshotDirectory() {
		return SignedPaintingsClient.client.gameDirectory+"\\"+ Screenshot.SCREENSHOT_DIR+"\\";
	}

	public static void info(String s, boolean force) {
		if (loggingEnabled || force) LOGGER.info(s);
	}

	public static String getDomain(String url) {
		int start = url.indexOf('/')+2;
		return url.substring(0, url.substring(start).indexOf('/')+start+1);
	}

	public static Style getUrlButton(String url) {
		try {
			return Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(new URI(url)));
		} catch (Exception ignored) {
			return Style.EMPTY;
		}
	}
}