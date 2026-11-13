package com.sarexstudio.cinematics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 *  /sarexstudio escena <nombre> [jugadores]  -> reproduce la escena (a todos por defecto)
 *  /sarexstudio detener [jugadores]          -> corta la escena en curso
 *  /sarexstudio lista                        -> escenas disponibles
 */
public class SarexCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("sarexstudio")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("escena")
                    .then(Commands.argument("nombre", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Scenes.names(), b))
                        .executes(ctx -> play(ctx, ctx.getSource().getServer().getPlayerList().getPlayers()))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                            .executes(ctx -> play(ctx, EntityArgument.getPlayers(ctx, "jugadores"))))))
                .then(Commands.literal("detener")
                    .executes(ctx -> stop(ctx, ctx.getSource().getServer().getPlayerList().getPlayers()))
                    .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(ctx -> stop(ctx, EntityArgument.getPlayers(ctx, "jugadores")))))
                .then(Commands.literal("lista").executes(SarexCommands::list))
        );
    }

    private static int send(Collection<ServerPlayer> targets, PlayScenePayload payload) {
        int n = 0;
        for (ServerPlayer p : targets) {
            // solo a quien tenga el mod (los demas no ven nada y no se desconectan)
            if (ServerPlayNetworking.canSend(p, PlayScenePayload.TYPE)) {
                ServerPlayNetworking.send(p, payload);
                n++;
            }
        }
        return n;
    }

    private static int play(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String name = StringArgumentType.getString(ctx, "nombre");
        if (!Scenes.all().containsKey(name)) {
            ctx.getSource().sendFailure(Component.literal("Escena desconocida: " + name + ". Usa /sarexstudio lista"));
            return 0;
        }
        int n = send(targets, new PlayScenePayload(name, false));
        ctx.getSource().sendSuccess(() -> Component.literal("Escena '" + name + "' enviada a " + n + " jugador(es)."), true);
        return n;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        int n = send(targets, new PlayScenePayload("", true));
        ctx.getSource().sendSuccess(() -> Component.literal("Escena detenida."), true);
        return n;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Scenes.all().forEach((n, d) -> ctx.getSource().sendSuccess(
                () -> Component.literal("- " + n + (d.isEmpty() ? "" : "  (" + d + ")")), false));
        return Scenes.all().size();
    }
}
