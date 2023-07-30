package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.*;

public class UploadCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> uploadNode = ClientCommandManager
                .literal("paintings:upload")
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .executes(UploadCommand::upload)
                )
                .build();

        return uploadNode;
    }

    private static int upload(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.upload", url).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
        );
        SignedPaintingsClient.say(text);
        SignedPaintingsClient.uploadManager.uploadToImgur(url, UploadCommand::onLoad);
        return 1;
    }

    private static void onLoad(String link) {
        if (link != null) {
            MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.upload.done", link).setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
            );
            SignedPaintingsClient.say(text);
        } else {
            SignedPaintingsClient.say("commands.upload.fail");
        }
    }
}
