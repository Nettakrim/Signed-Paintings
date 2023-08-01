package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class ToggleCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> toggleNode = ClientCommandManager
                .literal("paintings:toggle")
                .executes(ToggleCommand::toggleAll)
                .build();

        LiteralCommandNode<FabricClientCommandSource> signNode = ClientCommandManager
                .literal("signs")
                .executes(ToggleCommand::toggleSigns)
                .build();

        LiteralCommandNode<FabricClientCommandSource> bannersNode = ClientCommandManager
                .literal("banners")
                .executes(ToggleCommand::toggleBanners)
                .build();

        LiteralCommandNode<FabricClientCommandSource> shieldsNode = ClientCommandManager
                .literal("shields")
                .executes(ToggleCommand::toggleShields)
                .build();

        toggleNode.addChild(signNode);
        toggleNode.addChild(bannersNode);
        toggleNode.addChild(shieldsNode);

        return toggleNode;
    }

    private static int toggleAll(CommandContext<FabricClientCommandSource> context) {
        boolean value = !(SignedPaintingsClient.renderSigns || SignedPaintingsClient.renderBanners || SignedPaintingsClient.renderShields);
        SignedPaintingsClient.renderSigns = value;
        SignedPaintingsClient.renderBanners = value;
        SignedPaintingsClient.renderShields = value;
        return 1;
    }

    private static int toggleSigns(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderSigns = !SignedPaintingsClient.renderSigns;
        return 1;
    }

    private static int toggleBanners(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderBanners = !SignedPaintingsClient.renderBanners;
        return 1;
    }

    private static int toggleShields(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderShields = !SignedPaintingsClient.renderShields;
        return 1;
    }
}
