package me.onethecrazy;

import me.onethecrazy.commands.Commands;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.save.AllTheSkinsSave;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;


import java.io.IOException;

public class AllTheSkinsClient implements ClientModInitializer {
	@Nullable private static AllTheSkinsSave options;
	public static String bannerText;
	public static boolean isFirstStartup;

	public static AllTheSkinsSave options(){
		try{
			if(options == null)
				options = FileUtil.loadSave();
		} catch (IOException e) {
            AllTheSkins.LOGGER.error("Error while getting save: {0}", e);
        }

		return options;
    }

	@Override
	public void onInitializeClient() {
		firstStartupSetup();
		// Load Banner text
		BackendInteractor.getBannerTextAsync().thenAccept(text -> bannerText = text);
		// Initialize Commands
		registerCommands();
		// Register player join world callback
		registerPlayerJoinCallback();
		// Queue a self skin load
		queueLoadSelf();
	}

	public void registerCommands(){
		Commands.initializeCommands();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(Commands.SKINS_COMMAND);
		});
	}

	public void registerPlayerJoinCallback(){
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			// Reload that players skin when they (re-)join the world
			if (entity instanceof OtherClientPlayerEntity other) {
				SkinManager.loadSkin(other.getUuidAsString());
			}
		});
	}

	public void firstStartupSetup(){
		isFirstStartup = !FileUtil.doesFileExist(FileUtil.getSavePath());

		if(isFirstStartup)
			// Create Paths
			FileUtil.createPaths();
	}

	public void queueLoadSelf(){
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// Load self skin
			SkinManager.loadSelfSkin();
		});
	}
}