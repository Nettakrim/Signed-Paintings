package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class ToggleCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> toggleNode = ClientCommands
                .literal("paintings:toggle")
                .executes(ToggleCommand::toggleAll)
                .build();

        LiteralCommandNode<FabricClientCommandSource> allNode = ClientCommands
                .literal("all")
                .executes(ToggleCommand::toggleAll)
                .build();

        LiteralCommandNode<FabricClientCommandSource> signNode = ClientCommands
                .literal("signs")
                .executes(ToggleCommand::toggleSigns)
                .build();

        LiteralCommandNode<FabricClientCommandSource> bannersNode = ClientCommands
                .literal("banners")
                .executes(ToggleCommand::toggleBanners)
                .build();

        LiteralCommandNode<FabricClientCommandSource> shieldsNode = ClientCommands
                .literal("shields")
                .executes(ToggleCommand::toggleShields)
                .build();

        LiteralCommandNode<FabricClientCommandSource> cullingNode = ClientCommands
                .literal("culling")
                .executes(ToggleCommand::toggleCulling)
                .build();

        toggleNode.addChild(allNode);
        toggleNode.addChild(signNode);
        toggleNode.addChild(bannersNode);
        toggleNode.addChild(shieldsNode);
        toggleNode.addChild(cullingNode);
        return toggleNode;
    }

    private static int toggleAll(CommandContext<FabricClientCommandSource> context) {
        boolean value = !(SignedPaintingsClient.renderSigns || SignedPaintingsClient.renderBanners || SignedPaintingsClient.renderShields);
        SignedPaintingsClient.renderSigns = value;
        SignedPaintingsClient.renderBanners = value;
        SignedPaintingsClient.renderShields = value;
        sayMessage("all", value);
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static int toggleSigns(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderSigns = !SignedPaintingsClient.renderSigns;
        sayMessage("signs", SignedPaintingsClient.renderSigns);
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static int toggleBanners(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderBanners = !SignedPaintingsClient.renderBanners;
        sayMessage("banners", SignedPaintingsClient.renderBanners);
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static int toggleShields(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.renderShields = !SignedPaintingsClient.renderShields;
        sayMessage("shields", SignedPaintingsClient.renderShields);
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static int toggleCulling(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.reduceCulling = !SignedPaintingsClient.reduceCulling;
        sayMessage("culling", SignedPaintingsClient.reduceCulling);
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static void sayMessage(String key, boolean isOn) {
        SignedPaintingsClient.sayTranslated("commands.toggle."+key+(isOn ? ".on" : ".off"));
    }
}
