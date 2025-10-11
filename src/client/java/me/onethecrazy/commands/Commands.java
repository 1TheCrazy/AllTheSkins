package me.onethecrazy.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.onethecrazy.screens.ConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;


public class Commands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> SKINS_COMMAND;

    public static void initializeCommands(){
        SKINS_COMMAND = ClientCommandManager.literal("skin")
                        .executes(context -> waypointsCommandHandler());
                //.then(ClientCommandManager.literal("copy")
                //       .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> waypointsCommandHandler(context, AllTheSkinsCommandType.COPY)))
                //);
    }

    private static int waypointsCommandHandler(){
        MinecraftClient.getInstance().send(() ->
                MinecraftClient.getInstance().setScreen(new ConfigScreen())
        );
        return 1;
    }
}
