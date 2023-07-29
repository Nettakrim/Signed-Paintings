package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class RefreshCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> refreshNode = ClientCommandManager
                .literal("paintings:refresh")
                .executes(RefreshCommand::refresh)
                .build();

        return refreshNode;
    }

    private static int refresh(CommandContext<FabricClientCommandSource> context) {
        int amount = SignedPaintingsClient.imageManager.clear();
        SignedPaintingsClient.say("commands.refreshed", Integer.toString(amount));
        return 1;
    }
}
