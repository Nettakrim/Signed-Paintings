package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.RootCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SignedPaintingsCommands {
    public static final SuggestionProvider<FabricClientCommandSource> images = (context, builder) -> {
        if (SignedPaintingsClient.imageManager.getUrlSuggestions(builder) >= 2) {
            builder.suggest("all");
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static ArrayList<String> recentScreenshots;
    public static final SuggestionProvider<FabricClientCommandSource> screenshots = (context, builder) -> {
        for (String screenshot : recentScreenshots) {
            builder.suggest(screenshot);
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void initialize() {
        recentScreenshots = new ArrayList<>();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            RootCommandNode<FabricClientCommandSource> root = dispatcher.getRoot();

            root.addChild(BlockCommand.getCommandNode());
            root.addChild(ReloadCommand.getCommandNode());
            root.addChild(StatusCommand.getCommandNode());
            root.addChild(ToggleCommand.getCommandNode());
            root.addChild(UploadCommand.getCommandNode());
        });
    }
}
