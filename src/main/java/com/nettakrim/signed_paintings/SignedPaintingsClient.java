package com.nettakrim.signed_paintings;

import com.nettakrim.signed_paintings.gui.SignEditingInfo;
import com.nettakrim.signed_paintings.rendering.PaintingRenderer;
import com.nettakrim.signed_paintings.util.ImageManager;
import com.nettakrim.signed_paintings.util.URLAlias;
import com.nettakrim.signed_paintings.util.UploadManager;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
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

	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();

		imageManager = new ImageManager();
		paintingRenderer = new PaintingRenderer();

		imageManager.registerURLAlias(new URLAlias("https://i.imgur.com/", new String[]{"imgur.com/","imgur:"}, ".png"));

		uploadManager = new UploadManager("c1802a39166b9d0");
	}

	public static String combineSignText(SignText text) {
		Text[] layers = text.getMessages(false);
		String combined = layers[0].getString();
		combined += layers[1].getString();
		combined += layers[2].getString();
		combined += layers[3].getString();
		return combined;
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
}