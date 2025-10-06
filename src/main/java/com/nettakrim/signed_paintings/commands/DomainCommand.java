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

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class DomainCommand {
    public static final SuggestionProvider<FabricClientCommandSource> disallow = (context, builder) -> {
        if (SignedPaintingsClient.imageManager.allowedDomains.contains("https://")) {
            builder.suggest("anything");
            return CompletableFuture.completedFuture(builder.build());
        }

        for (String url : SignedPaintingsClient.imageManager.allowedDomains) {
            builder.suggest(url);
        }
        if (SignedPaintingsClient.imageManager.allowedDomains.size() >= 2) {
            builder.suggest("all_allowed");
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static final SuggestionProvider<FabricClientCommandSource> allow = (context, builder) -> {
        for (String url : SignedPaintingsClient.imageManager.blockPromptedDomains) {
            builder.suggest(url);
        }
        if (SignedPaintingsClient.imageManager.blockPromptedDomains.size() >= 2) {
            builder.suggest("all_prompted");
        }
        builder.suggest("anything", Text.translatable(SignedPaintingsClient.MODID+".domain.warning"));
        return CompletableFuture.completedFuture(builder.build());
    };

    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> domainNode = ClientCommandManager
                .literal("paintings:domain")
                .build();

        LiteralCommandNode<FabricClientCommandSource> allowNode = ClientCommandManager
                .literal("allow")
                .then(
                        ClientCommandManager.argument("domain", StringArgumentType.greedyString())
                                .suggests(allow)
                                .executes(DomainCommand::allow)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> blockNode = ClientCommandManager
                .literal("disallow")
                .then(
                        ClientCommandManager.argument("domain", StringArgumentType.greedyString())
                                .suggests(disallow)
                                .executes(DomainCommand::disallow)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> listNode = ClientCommandManager
                .literal("list")
                .executes(DomainCommand::list)
                .build();

        domainNode.addChild(allowNode);
        domainNode.addChild(blockNode);
        domainNode.addChild(listNode);
        return domainNode;
    }

    private static int allow(CommandContext<FabricClientCommandSource> context) {
        String domain = StringArgumentType.getString(context, "domain");
        if (domain.equals("anything") || domain.equals("https://")) {
            Text warning = Text.translatable(SignedPaintingsClient.MODID+".domain.warning");
            if (SignedPaintingsClient.imageManager.allowDomain("https://")) {
                SignedPaintingsClient.sayTranslated("commands.domain.allow.anything", warning);
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.allow.anything.exists", warning);
        } else if (domain.equals("all_prompted")) {
            int count = 0;
            for (String prompted : new ArrayList<>(SignedPaintingsClient.imageManager.blockPromptedDomains)) {
                if (SignedPaintingsClient.imageManager.allowDomain(prompted)) {
                    count++;
                }
            }
            SignedPaintingsClient.sayTranslated("commands.domain.allow.all", String.valueOf(count));
            return count;
        } else if (domain.contains("//")) {
            if (SignedPaintingsClient.imageManager.allowDomain(domain)) {
                SignedPaintingsClient.sayTranslated("commands.domain.allow", domain);
                if (domain.equals("http://")) {
                    SignedPaintingsClient.sayRaw(Text.translatable(SignedPaintingsClient.MODID+".domain.warning").setStyle(Style.EMPTY.withColor(SignedPaintingsClient.textColor)));
                }
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.allow.exists", domain);
        } else {
            SignedPaintingsClient.sayTranslated("commands.domain.invalid", domain);
        }
        return 0;
    }

    private static int disallow(CommandContext<FabricClientCommandSource> context) {
        String domain = StringArgumentType.getString(context, "domain");
        if (domain.equals("anything") || domain.equals("https://")) {
            // remove http for good measure, if explicitly removing it, then the usual // case will catch it
            SignedPaintingsClient.imageManager.removeDomain("http://");

            if (SignedPaintingsClient.imageManager.removeDomain("https://")) {
                SignedPaintingsClient.sayTranslated("commands.domain.disallow.anything");
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.disallow.anything.missing");
        } else if (domain.equals("all_allowed")) {
            int count = 0;
            for (String allowed : new ArrayList<>(SignedPaintingsClient.imageManager.allowedDomains)) {
                if (SignedPaintingsClient.imageManager.removeDomain(allowed)) {
                    count++;
                }
            }
            SignedPaintingsClient.sayTranslated("commands.domain.disallow.all", String.valueOf(count));
            SignedPaintingsClient.imageManager.allowedDomains.clear();
            SignedPaintingsClient.imageManager.reloadAll();
            return count;
        } else if (domain.contains("//")) {
            if (SignedPaintingsClient.imageManager.removeDomain(domain)) {
                SignedPaintingsClient.sayTranslated("commands.domain.disallow", domain);
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.disallow.missing", domain);
        } else {
            SignedPaintingsClient.sayTranslated("commands.domain.invalid", domain);
        }
        return 0;
    }

    private static int list(CommandContext<FabricClientCommandSource> context) {
        MutableText text = Text.translatable(SignedPaintingsClient.MODID+".commands.domain.list.start");
        for (String domain : SignedPaintingsClient.imageManager.allowedDomains) {
            if (!domain.equals("https://")) {
                text.append(Text.translatable(SignedPaintingsClient.MODID + ".commands.domain.list", domain).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, domain))));
            }
        }
        if (SignedPaintingsClient.imageManager.allowedDomains.isEmpty()) {
            text.append(Text.translatable(SignedPaintingsClient.MODID+".commands.domain.list.none"));
        } else if (SignedPaintingsClient.imageManager.allowedDomains.contains("https://")) {
            text.append(Text.translatable(SignedPaintingsClient.MODID + ".commands.domain.list.anything", Text.translatable(SignedPaintingsClient.MODID + ".domain.warning")));
        }

        SignedPaintingsClient.longSay(text);
        return SignedPaintingsClient.imageManager.allowedDomains.size();
    }
}