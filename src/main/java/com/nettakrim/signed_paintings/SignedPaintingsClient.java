package com.nettakrim.signed_paintings;

import com.nettakrim.signed_paintings.commands.SignedPaintingsCommands;
import com.nettakrim.signed_paintings.gui.SignEditingInfo;
import com.nettakrim.signed_paintings.rendering.PaintingRenderer;
import com.nettakrim.signed_paintings.util.ImageManager;
import com.nettakrim.signed_paintings.util.URLAlias;
import com.nettakrim.signed_paintings.util.UploadManager;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static MinecraftClient client;

	public static ImageManager imageManager;
	public static UploadManager uploadManager;
	public static PaintingRenderer paintingRenderer;

	public static SignEditingInfo currentSignEdit;

	public static final TextColor textColor = TextColor.fromRgb(0xAAAAAA);
	public static final TextColor nameTextColor = TextColor.fromRgb(0x4BCCA3);

	public static boolean renderSigns;
	public static boolean renderBanners;
	public static boolean renderShields;

	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();

		imageManager = new ImageManager();
		imageManager.registerURLAlias(new URLAlias("https://i.imgur.com/", new String[]{"i.imgur.com/","imgur.com/","imgur:"}, ".png"));

		paintingRenderer = new PaintingRenderer();
		renderSigns = true;
		renderBanners = true;
		renderShields = true;

		uploadManager = new UploadManager("c1802a39166b9d0");

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

		int low = 0;
		int high = reference.length();
		int index = Integer.MAX_VALUE;

		while (low <= high) {
			int mid = low  + ((high - low) / 2);
			int currentWidth = textRenderer.getWidth(reference.substring(0, mid));
			if (currentWidth < budgetWidth) {
				low = mid + 1;
			} else if (currentWidth > budgetWidth) {
				high = mid - 1;
			} else if (currentWidth == budgetWidth) {
				return mid;
			}
			index = mid;
		}
		//length was not directly achievable, so use the next smallest length instead
		if (textRenderer.getWidth(reference.substring(0, index)) > budgetWidth) index--;
		return index;
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

	public static void say(String key, Object... args) {
		if (client.player == null) return;
		Text text = Text.translatable(MODID+".say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(Text.translatable(MODID+"."+key, args).setStyle(Style.EMPTY.withColor(textColor)));
		client.player.sendMessage(text);
	}

	public static void sayStyled(String key, Style style, Object... args) {
		if (client.player == null) return;
		Text text = Text.translatable(MODID+".say").setStyle(style.withColor(nameTextColor)).append(Text.translatable(MODID+"."+key, args).setStyle(Style.EMPTY.withColor(textColor)));
		client.player.sendMessage(text);
	}

	public static void say(MutableText text) {
		if (client.player == null) return;
		Text newText = Text.translatable(MODID+".say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		client.player.sendMessage(newText);
	}

	public static void sayRaw(MutableText text) {
		if (client.player == null) return;
		client.player.sendMessage(text);
	}

	public static void longSay(MutableText text) {
		if (client.player == null) return;
		Text newText = Text.translatable(MODID+".long_say").setStyle(Style.EMPTY.withColor(nameTextColor)).append(text.setStyle(text.getStyle().withColor(textColor)));
		client.player.sendMessage(newText);
	}
}