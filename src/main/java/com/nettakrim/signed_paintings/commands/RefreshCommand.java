package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class RefreshCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> refreshNode = ClientCommandManager
                .literal("paintings:refresh")
                .executes(RefreshCommand::refreshAll)
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests(SignedPaintingsCommands.images)
                        .executes(RefreshCommand::refresh)
                )
                .build();

        return refreshNode;
    }

    private static int refreshAll(CommandContext<FabricClientCommandSource> context) {
        int amount = SignedPaintingsClient.imageManager.clearAll();
        SignedPaintingsClient.say("commands.refreshed", Integer.toString(amount));
        return 1;
    }

    private static int refresh(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        if (url.equals("all")) return refreshAll(context);
        int amount = SignedPaintingsClient.imageManager.clearUrl(url);
        SignedPaintingsClient.say("commands.refreshed", Integer.toString(amount));
        return 1;
    }
}
