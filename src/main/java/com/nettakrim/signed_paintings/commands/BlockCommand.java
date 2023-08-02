package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class BlockCommand {
    public static final SuggestionProvider<FabricClientCommandSource> blocked = (context, builder) -> {
        for (String url : SignedPaintingsClient.imageManager.blockedURLs) {
            builder.suggest(url);
        }
        if (SignedPaintingsClient.imageManager.blockedURLs.size() >= 2) {
            builder.suggest("all");
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> blockNode = ClientCommandManager
                .literal("paintings:block")
                .build();

        LiteralCommandNode<FabricClientCommandSource> addNode = ClientCommandManager
                .literal("add")
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests(SignedPaintingsCommands.images)
                        .executes(BlockCommand::block)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> removeNode = ClientCommandManager
                .literal("remove")
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests(blocked)
                        .executes(BlockCommand::unblock)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> listNode = ClientCommandManager
                .literal("list")
                .executes(BlockCommand::list)
                .build();

        LiteralCommandNode<FabricClientCommandSource> autoNode = ClientCommandManager
                .literal("auto")
                .executes(BlockCommand::auto)
                .build();

        blockNode.addChild(addNode);
        blockNode.addChild(removeNode);
        blockNode.addChild(listNode);
        blockNode.addChild(autoNode);
        return blockNode;
    }

    private static int block(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        if (url.equals("all")) {
            SignedPaintingsClient.imageManager.blockedURLs.addAll(SignedPaintingsClient.imageManager.getUrls());
            MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.block.add.all");
            for (String newBlock : SignedPaintingsClient.imageManager.getUrls()) {
                text.append(Text.translatable(SignedPaintingsClient.MODID+".commands.block.list", newBlock).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, newBlock))));
            }
            SignedPaintingsClient.imageManager.reloadAll();
            SignedPaintingsClient.longSay(text);
        } else {
            SignedPaintingsClient.imageManager.blockedURLs.add(url);
            SignedPaintingsClient.imageManager.reloadUrl(url);
            SignedPaintingsClient.sayStyled("commands.block.add", Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)), url);
        }
        SignedPaintingsClient.imageManager.makeChange();
        return 1;
    }

    private static int unblock(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        boolean success = true;
        if (SignedPaintingsClient.imageManager.autoBlockNew) auto(context);
        if (url.equals("all")) {
            SignedPaintingsClient.imageManager.blockedURLs.clear();
            SignedPaintingsClient.imageManager.reloadAll();
            SignedPaintingsClient.say("commands.block.remove.all");
        } else {
            success = SignedPaintingsClient.imageManager.blockedURLs.remove(url);
            SignedPaintingsClient.imageManager.reloadUrl(url);
            SignedPaintingsClient.sayStyled("commands.block.remove", Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)), url);
        }
        if (success) SignedPaintingsClient.imageManager.makeChange();
        return success ? 1 : 0;
    }

    private static int list(CommandContext<FabricClientCommandSource> context) {
        MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.block.list.start");
        for (String url : SignedPaintingsClient.imageManager.blockedURLs) {
            text.append(Text.translatable(SignedPaintingsClient.MODID+".commands.block.list", url).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
        }
        if (SignedPaintingsClient.imageManager.blockedURLs.size() == 0) {
            text.append(Text.translatable(SignedPaintingsClient.MODID+".commands.block.list.none"));
        }
        SignedPaintingsClient.longSay(text);
        return 1;
    }

    private static int auto(CommandContext<FabricClientCommandSource> context) {
        SignedPaintingsClient.imageManager.autoBlockNew = !SignedPaintingsClient.imageManager.autoBlockNew;
        if (SignedPaintingsClient.imageManager.autoBlockNew) {
            SignedPaintingsClient.longSay(Text.translatable(SignedPaintingsClient.MODID+".commands.block.auto.on"));
        } else {
            SignedPaintingsClient.say("commands.block.auto.off");
        }
        return 1;
    }
}
