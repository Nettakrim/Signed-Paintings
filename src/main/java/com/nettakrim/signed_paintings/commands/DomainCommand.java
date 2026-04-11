package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.DomainWarningScreen;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class DomainCommand {
    public static final SuggestionProvider<FabricClientCommandSource> untrust = (context, builder) -> {
        if (SignedPaintingsClient.imageManager.trustedDomains.contains("https://")) {
            builder.suggest("anything");
            return CompletableFuture.completedFuture(builder.build());
        }

        for (String url : SignedPaintingsClient.imageManager.trustedDomains) {
            builder.suggest(url);
        }
        if (SignedPaintingsClient.imageManager.trustedDomains.size() >= 2) {
            builder.suggest("all_trusted");
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static final SuggestionProvider<FabricClientCommandSource> trust = (context, builder) -> {
        for (String url : SignedPaintingsClient.imageManager.blockPromptedDomains) {
            builder.suggest(url);
        }
        if (SignedPaintingsClient.imageManager.blockPromptedDomains.size() >= 2) {
            builder.suggest("all_prompted");
        }
        builder.suggest("anything", Component.translatable(SignedPaintingsClient.MODID+".domain.warning"));
        return CompletableFuture.completedFuture(builder.build());
    };

    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> domainNode = ClientCommands
                .literal("paintings:domain")
                .build();

        LiteralCommandNode<FabricClientCommandSource> trustNode = ClientCommands
                .literal("trust")
                .then(
                        ClientCommands.argument("domain", StringArgumentType.greedyString())
                                .suggests(trust)
                                .executes(DomainCommand::trust)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> untrustNode = ClientCommands
                .literal("untrust")
                .then(
                        ClientCommands.argument("domain", StringArgumentType.greedyString())
                                .suggests(untrust)
                                .executes(DomainCommand::untrust)
                )
                .build();

        LiteralCommandNode<FabricClientCommandSource> listNode = ClientCommands
                .literal("list")
                .executes(DomainCommand::list)
                .build();

        domainNode.addChild(trustNode);
        domainNode.addChild(untrustNode);
        domainNode.addChild(listNode);
        return domainNode;
    }

    private static int trust(CommandContext<FabricClientCommandSource> context) {
        String domainTemp = StringArgumentType.getString(context, "domain");
        String domain;
        if (domainTemp.equals("anything")) {
            domain = "https://";
        } else {
            domain = domainTemp;
        }

        if (domain.endsWith("//")) {
            Component warning = Component.translatable(SignedPaintingsClient.MODID+".domain.warning");
            if (SignedPaintingsClient.imageManager.trustedDomains.contains(domain)) {
                SignedPaintingsClient.sayTranslated("commands.domain.trust.anything.exists", warning);
                return 0;
            }

            SignedPaintingsClient.client.schedule(() -> SignedPaintingsClient.client.setScreen(new DomainWarningScreen(domain, DomainCommand::confirmAnything)));
            return 1;
        } else if (domain.equals("all_prompted")) {
            int count = 0;
            for (String prompted : new ArrayList<>(SignedPaintingsClient.imageManager.blockPromptedDomains)) {
                if (SignedPaintingsClient.imageManager.trustDomain(prompted)) {
                    count++;
                }
            }
            SignedPaintingsClient.sayTranslated("commands.domain.trust.all", String.valueOf(count));
            return count;
        } else if (domain.contains("//")) {
            if (SignedPaintingsClient.imageManager.trustDomain(domain)) {
                SignedPaintingsClient.sayTranslated("commands.domain.trust", domain);
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.trust.exists", domain);
        } else {
            SignedPaintingsClient.sayTranslated("commands.domain.invalid", domain);
        }
        return 0;
    }

    private static void confirmAnything(String domain) {
        MutableComponent warning = Component.translatable(SignedPaintingsClient.MODID+".domain.warning");
        if (SignedPaintingsClient.imageManager.trustDomain(domain)) {
            if (domain.equals("https://")) {
                SignedPaintingsClient.sayTranslated("commands.domain.trust.anything", warning);
            } else {
                SignedPaintingsClient.sayTranslated("commands.domain.trust", domain);
                SignedPaintingsClient.sayRaw(warning.setStyle(Style.EMPTY.withColor(SignedPaintingsClient.textColor)));
            }
        } else {
            if (domain.equals("https://")) {
                SignedPaintingsClient.sayTranslated("commands.domain.trust.anything.exists", warning);
            } else {
                SignedPaintingsClient.sayTranslated("commands.domain.trust.exists", domain);
            }
        }
    }

    private static int untrust(CommandContext<FabricClientCommandSource> context) {
        String domain = StringArgumentType.getString(context, "domain");
        if (domain.equals("anything") || domain.equals("https://")) {
            // remove http for good measure, if explicitly removing it, then the usual // case will catch it
            SignedPaintingsClient.imageManager.untrustDomain("http://");

            if (SignedPaintingsClient.imageManager.untrustDomain("https://")) {
                SignedPaintingsClient.sayTranslated("commands.domain.untrust.anything");
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.untrust.anything.missing");
        } else if (domain.equals("all_trusted")) {
            int count = 0;
            for (String trusted : new ArrayList<>(SignedPaintingsClient.imageManager.trustedDomains)) {
                if (SignedPaintingsClient.imageManager.untrustDomain(trusted)) {
                    count++;
                }
            }
            SignedPaintingsClient.sayTranslated("commands.domain.untrust.all", String.valueOf(count));
            SignedPaintingsClient.imageManager.trustedDomains.clear();
            SignedPaintingsClient.imageManager.reloadAll();
            return count;
        } else if (domain.contains("//")) {
            if (SignedPaintingsClient.imageManager.untrustDomain(domain)) {
                SignedPaintingsClient.sayTranslated("commands.domain.untrust", domain);
                return 1;
            }
            SignedPaintingsClient.sayTranslated("commands.domain.untrust.missing", domain);
        } else {
            SignedPaintingsClient.sayTranslated("commands.domain.invalid", domain);
        }
        return 0;
    }

    private static int list(CommandContext<FabricClientCommandSource> context) {
        MutableComponent text = Component.translatable(SignedPaintingsClient.MODID+".commands.domain.list.start");
        for (String domain : SignedPaintingsClient.imageManager.trustedDomains) {
            if (!domain.equals("https://")) {
                text.append(Component.translatable(SignedPaintingsClient.MODID + ".commands.domain.list", domain).setStyle(SignedPaintingsClient.getUrlButton(domain)));
            }
        }
        if (SignedPaintingsClient.imageManager.trustedDomains.isEmpty()) {
            text.append(Component.translatable(SignedPaintingsClient.MODID+".commands.domain.list.none"));
        } else if (SignedPaintingsClient.imageManager.trustedDomains.contains("https://")) {
            text.append(Component.translatable(SignedPaintingsClient.MODID + ".commands.domain.list.anything", Component.translatable(SignedPaintingsClient.MODID + ".domain.warning")));
        }

        SignedPaintingsClient.longSay(text);
        return SignedPaintingsClient.imageManager.trustedDomains.size();
    }
}