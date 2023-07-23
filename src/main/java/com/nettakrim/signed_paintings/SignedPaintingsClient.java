package com.nettakrim.signed_paintings;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static MinecraftClient client;

	public static ImageManager imageManager;
	public static PaintingRenderer paintingRenderer;

	public static SignEditingInfo currentSignEdit;


	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();

		imageManager = new ImageManager();
		paintingRenderer = new PaintingRenderer();

		imageManager.registerURLAlias(new URLAlias("https://i.imgur.com/", new String[]{"imgur.com/","imgur:"}, ".png"));
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
}