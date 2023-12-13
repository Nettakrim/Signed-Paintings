package com.nettakrim.signed_paintings.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageStatus;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;

public class StatusCommand {
    public static LiteralCommandNode<FabricClientCommandSource> getCommandNode() {
        LiteralCommandNode<FabricClientCommandSource> statusNode = ClientCommandManager
                .literal("paintings:status")
                .executes(StatusCommand::statusAll)
                .then(
                        ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests(SignedPaintingsCommands.images)
                        .executes(StatusCommand::status)
                )
                .build();

        return statusNode;
    }

    private static int statusAll(CommandContext<FabricClientCommandSource> context) {
        ArrayList<ImageStatus> statuses = SignedPaintingsClient.imageManager.getAllStatus();
        long totalSize = 0;
        statuses.sort(Collections.reverseOrder());
        for (ImageStatus status : statuses) {
            totalSize += status.getTotalSize();
        }

        MutableText text = getStatusText("unique", Integer.toString(statuses.size()));
        text.append(getStatusText("total_size", getKBString(totalSize)));
        for (ImageStatus status : statuses) {
            if (status.ready) {
                MutableText linkText = getStatusText("size.link", getKBString(status.getTotalSize()), status.url);
                linkText.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, status.url)));
                text.append(linkText);
            }
        }

        SignedPaintingsClient.longSay(text);
        return 1;
    }

    private static int status(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        if (url.equals("all")) return statusAll(context);
        ImageStatus status = SignedPaintingsClient.imageManager.getUrlStatus(url);
        if (status == null || !status.ready) {
            SignedPaintingsClient.say("commands.status.none", url);
            return 0;
        }

        MutableText text = Text.literal("").append(getStatusText("link", status.url).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, status.url))));
        text.append(getStatusText("resolutions", Integer.toString(status.getResolutionsCount())));
        text.append(getStatusText("total_size", getKBString(status.getTotalSize())));

        status.resolutionStatuses.sort(Collections.reverseOrder());
        for (ImageStatus.ResolutionStatus r : status.resolutionStatuses) {
            String size = r.pixels().x+"x"+r.pixels().y;
            text.append(getStatusText("size."+(r.isScaled() ? "scaled" : "default"), getKBString(r.bytes()), size));
        }

        SignedPaintingsClient.longSay(text);
        return 1;
    }

    private static String getKBString(long bytes) {
        String s = Float.toString(SignedPaintingsClient.roundFloatTo3DP(bytes/1000f));
        int index = s.indexOf('.');
        if (index >= 4) return s.substring(0, index);
        return s.substring(0, Math.min(5, s.length()));
    }

    private static MutableText getStatusText(String key, Object... args) {
        return Text.translatable(SignedPaintingsClient.MODID+".commands.status."+key, args);
    }
}
