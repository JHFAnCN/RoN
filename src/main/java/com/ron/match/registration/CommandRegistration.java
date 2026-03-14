package com.ron.match.registration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ron.match.server.MatchServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommandRegistration {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent evt) {
        CommandDispatcher<CommandSourceStack> d = evt.getDispatcher();
        d.register(Commands.literal("rts-preparationhall")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                if (src.getEntity() instanceof ServerPlayer self) {
                    MatchServerEvents.joinPreparationHall(self);
                    return 1;
                }
                src.sendFailure(net.minecraft.network.chat.Component.literal("该指令只能由玩家执行，或指定目标玩家。"));
                return 0;
            })
            .then(Commands.argument("target", StringArgumentType.string())
                .suggests((ctx, sb) -> {
                    ctx.getSource().getServer().getPlayerList().getPlayers()
                        .forEach(p -> sb.suggest(p.getName().getString()));
                    return sb.buildFuture();
                })
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    String name = StringArgumentType.getString(ctx, "target");
                    ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(name);
                    if (target != null) {
                        MatchServerEvents.joinPreparationHall(target);
                        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("已让 " + name + " 加入准备大厅。"), true);
                        return 1;
                    }
                    src.sendFailure(net.minecraft.network.chat.Component.literal("未找到玩家: " + name));
                    return 0;
                })));
        d.register(Commands.literal("rts-kick")
            .requires(s -> s.hasPermission(2))
            .then(Commands.argument("target", StringArgumentType.string())
                .suggests((ctx, sb) -> {
                    ctx.getSource().getServer().getPlayerList().getPlayers()
                        .forEach(p -> sb.suggest(p.getName().getString()));
                    return sb.buildFuture();
                })
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    ServerPlayer executor = src.getEntity() instanceof ServerPlayer p ? p : null;
                    MatchServerEvents.kickFromPreparationHall(executor, targetName);
                    return 1;
                })));
        d.register(Commands.literal("rts-restmatch")
            .requires(s -> s.hasPermission(2))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                ServerPlayer player = src.getEntity() instanceof ServerPlayer p ? p : null;
                MatchServerEvents.handleResetMatch(player, player == null);
                return 1;
            }));
        d.register(Commands.literal("rts-match-settings")
            .requires(s -> s.hasPermission(2))
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    MatchServerEvents.openSettingsScreen(player);
                    return 1;
                }
                return 0;
            }));
    }
}
