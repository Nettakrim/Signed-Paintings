package com.nettakrim;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedPaintingsClient implements ClientModInitializer {
	public static final String MODID = "signed_paintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static MinecraftClient client;

	@Override
	public void onInitializeClient() {
		client = MinecraftClient.getInstance();
	}
}