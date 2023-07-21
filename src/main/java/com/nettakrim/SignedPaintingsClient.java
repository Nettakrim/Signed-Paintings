package com.nettakrim;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
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
	}

	public static String combineSignText(SignText text) {
		Text[] layers = text.getMessages(false);
		String combined = layers[0].getString();
		combined += layers[1].getString();
		combined += layers[2].getString();
		combined += layers[3].getString();
		return combined;
	}
}