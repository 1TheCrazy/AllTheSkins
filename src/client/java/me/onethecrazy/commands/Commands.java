package me.onethecrazy.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.ClientFileUtil;
import me.onethecrazy.SkinManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.Optional;


public class Commands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> SKINS_COMMAND;

    public static void initializeCommands(){
        SKINS_COMMAND = ClientCommandManager.literal("skin")
                .then(ClientCommandManager.literal("set")
                        .executes(context -> waypointsCommandHandler(context, AllTheSkinsCommandType.SET)))
                .then(ClientCommandManager.literal("toggle")
                        .executes(context -> waypointsCommandHandler(context, AllTheSkinsCommandType.TOGGLE)));
                //.then(ClientCommandManager.literal("copy")
                //       .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> waypointsCommandHandler(context, AllTheSkinsCommandType.COPY)))
                //);
    }

    private static int waypointsCommandHandler(CommandContext<FabricClientCommandSource> ctx, AllTheSkinsCommandType cmdType){
        switch(cmdType){
            case SET -> {
                SkinManager.pickClientSkin();
                return 1;
            }
            case TOGGLE -> {
                AllTheSkinsClient.options().isEnabled = !AllTheSkinsClient.options().isEnabled;
                return 1;
            }
            case COPY -> {
                // NOT Handled yet
                return 1;
            }
        }

        return 0;
    }

    private enum AllTheSkinsCommandType {
        SET,
        TOGGLE,
        COPY
    }
}
