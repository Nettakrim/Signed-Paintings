package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.*;

import java.io.File;

public class UploadCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> uploadNode = ClientCommandManager
                .literal("paintings:upload")
                .build();

        LiteralCommandNode<FabricClientCommandSource> urlNode = ClientCommandManager
                .literal("url")
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .executes(UploadCommand::uploadUrl)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> screenshotNode = ClientCommandManager
                .literal("screenshot")
                .then(
                        ClientCommandManager.argument("file", StringArgumentType.greedyString())
                        .suggests(SignedPaintingsCommands.screenshots)
                        .executes(UploadCommand::uploadScreenshot)
                )
                .build();

        uploadNode.addChild(urlNode);
        uploadNode.addChild(screenshotNode);
        return uploadNode;
    }

    private static int uploadUrl(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.upload", url).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
        );
        SignedPaintingsClient.say(text);
        SignedPaintingsClient.uploadManager.uploadUrlToImgur(url, UploadCommand::onLoad);
        return 1;
    }

    private static int uploadScreenshot(CommandContext<FabricClientCommandSource> context) {
        String filename = StringArgumentType.getString(context, "file");
        return uploadFile(SignedPaintingsClient.getScreenshotDirectory()+filename);
    }

    private static int uploadFile(String filename) {
        MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.upload", filename).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filename))
        );
        SignedPaintingsClient.say(text);
        SignedPaintingsClient.uploadManager.uploadFileToImgur(new File(filename), UploadCommand::onLoad);
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
