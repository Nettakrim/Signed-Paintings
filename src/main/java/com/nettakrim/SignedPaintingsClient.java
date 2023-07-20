package com.nettakrim;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static MinecraftClient client;

	public static ImageManager imageManager;

	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();

		imageManager = new ImageManager();

		String imageTest = "https://cdn.modrinth.com/data/bzJkPbG1/7651dbb4352c1e341c1bce51c3acb2e4348183d7.png";
		imageManager.registerImage(imageTest);
	}
}