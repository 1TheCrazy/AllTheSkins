package me.onethecrazy;

import me.onethecrazy.util.*;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.objects.save.Skin;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SkinManager {
    public static Map<String, @Nullable String> skinLookup = new HashMap<>();
    public static Map<String, @Nullable List<Vertex>> skinCache = new HashMap<>();

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static List<Vertex> selfSkinVertices() {
        String uuid = client.getSession().getUuidOrNull().toString();
        var cacheEntry = skinCache.get(uuid);

        return cacheEntry == null ? List.of() : cacheEntry;
    }

    public static void pickClientSkin(){

        // Open File picker dialogue
        ClientFileUtil.objPickerDialog()
                // Execute when user completes File-Selection
                .thenAccept(f -> {
                    if(f == null || Objects.equals(f, ""))
                        return;

                    try{
                        long fileSize = Files.size(Path.of(f));

                        long MAX_FILE_SIZE = 5L * 1024 * 1024;

                        // Restrict File size to 5mb
                        if(fileSize > MAX_FILE_SIZE){
                            ToastUtil.showFileTooLargeToast();
                            return;
                        }
                    }
                    catch(Exception ex) {
                        AllTheSkins.LOGGER.info("Ran into error while getting file size in client skin picker: {0}", ex);
                        return;
                    }

                    SkinManager.selectSelfSkin(f);
                });
    }

    public static void selectSelfSkin(String objPathString){
        try{
            String uuid = client.getSession().getUuidOrNull().toString();
            Path objPath = Path.of(objPathString);

            String obj = FileUtil.readOBJFile(objPath);
            String name = objPath.getFileName().toString();
            String hash = FileUtil.getSha256(obj);

            FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash), obj);

            AllTheSkinsClient.options().selectedSkin = new Skin(hash, name);

            // Save the updated options:
            FileUtil.writeSave(AllTheSkinsClient.options());

            // Reload self skin
            loadSelfSkin();

            // Send update to server
            BackendInteractor.setSkinOBJ(uuid, obj);
        }
        catch(Exception ex){
            AllTheSkins.LOGGER.info("Ran into error while setting self skin: {0}", ex);
        }
    }

    public static void resetSelfSkin(){
        String uuid = client.getSession().getUuidOrNull().toString();

        AllTheSkinsClient.options().selectedSkin = new Skin("", "");

        // Reload self skin
        loadSelfSkin();

        // Save in options
        FileUtil.writeSave(AllTheSkinsClient.options());

        // Send update to server
        BackendInteractor.setSkinOBJ(uuid, "");
    }

    public static void loadSelfSkin(){
        String uuid = client.getSession().getUuidOrNull().toString();

        // Set self skin to empty if we don't have a selected skin
        if(Objects.equals(AllTheSkinsClient.options().selectedSkin.id, "")){
            putLookupEntry(uuid, "");
            putCacheEntry(uuid, null);
            return;
        }

        // Load self skin
        try{
            Path objPath = FileUtil.getSkinPath(AllTheSkinsClient.options().selectedSkin.id);
            List<Vertex> vertices = ModelNormalizer.normalize(OBJParser.parse(objPath));

            putLookupEntry(uuid, AllTheSkinsClient.options().selectedSkin.id);
            putCacheEntry(uuid, vertices);
        }
        catch(Exception e){
            AllTheSkins.LOGGER.info("Ran into error while loading self skin obj content: {0}", e);
        }
    }

    public static void loadSkin(String uuid){
        // /getSkin
        // -> check local skins
        //      If not in local skins -> /files
        //          If response empty -> set entry to null
        //          Else -> entry to deserialized obj

        // Put uuid into cache so that we don't request for this uuid again in RenderMixin
        skinCache.put(uuid, null);
        skinLookup.put(uuid, null);

        // Player was never encountered before
        BackendInteractor.getSkinIDs(List.of(uuid))
                .thenAccept(map -> {
                    if(!map.containsKey(uuid))
                        putLookupEntry(uuid, null);
                    else
                        putLookupEntry(uuid, map.get(uuid));

                    // We don't have the skin loaded (or want it to be updated)
                    loadSkinIntoCache(uuid);
                });
    }

    private static void loadSkinIntoCache(String uuid){
        // Load from cache
        if(FileUtil.isSkinCached(skinLookup.get(uuid)))
        {
            try {
                List<Vertex> vertices = ModelNormalizer.normalize(
                        OBJParser.parse(
                                FileUtil.readOBJFile(
                                        FileUtil.getSkinPath(
                                                skinLookup.get(uuid)
                                        )
                                )
                        )
                );

                putCacheEntry(uuid, vertices);
            } catch (IOException e) {
                putCacheEntry(uuid, null);
                AllTheSkins.LOGGER.error("Ran into error while i/o loading skin from cache: {0}", e);
            }
        }
        // Request from Server
        else{
            BackendInteractor.getSkinOBJ(skinLookup.get(uuid))
                    .thenAccept(obj -> {
                        if(!Objects.equals(obj, "") && !Objects.equals(obj, null)){
                            // Try saving to local Cache
                            try {
                                String hash = skinLookup.get(uuid);

                                FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash), obj);
                            } catch (IOException e) {
                                AllTheSkins.LOGGER.error("Ran into error while saving skin to i/o cache: {0}", e);
                            }

                            List<Vertex> vertices = ModelNormalizer.normalize(OBJParser.parse(obj));
                            putCacheEntry(uuid, vertices);
                        }
                        else{
                            putCacheEntry(uuid, null);
                        }
                    });
        }
    }

    public static void putCacheEntry(String uuid, @Nullable List<Vertex> vertices){
        skinCache.put(uuid, vertices);
    }

    public static void putLookupEntry(String uuid, @Nullable String id){
        skinLookup.put(uuid, id);
    }
}
