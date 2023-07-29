package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.tree.RootCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class SignedPaintingsCommands {
    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            RootCommandNode<FabricClientCommandSource> root = dispatcher.getRoot();

            root.addChild(RefreshCommand.getCommandNode());
        });
    }
}
