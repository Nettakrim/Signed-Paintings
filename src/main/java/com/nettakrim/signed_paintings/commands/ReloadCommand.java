package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class ReloadCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> reloadNode = ClientCommandManager
                .literal("paintings:reload")
                .executes(ReloadCommand::reloadAll)
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests(SignedPaintingsCommands.images)
                        .executes(ReloadCommand::reload)
                )
                .build();

        return reloadNode;
    }

    private static int reloadAll(CommandContext<FabricClientCommandSource> context) {
        int amount = SignedPaintingsClient.imageManager.reloadAll();
        SignedPaintingsClient.say("commands.refreshed", Integer.toString(amount));
        return 1;
    }

    private static int reload(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        if (url.equals("all")) return reloadAll(context);
        int amount = SignedPaintingsClient.imageManager.reloadUrl(url);
        SignedPaintingsClient.say("commands.refreshed", Integer.toString(amount));
        return 1;
    }
}
