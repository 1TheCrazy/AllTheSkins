package me.onethecrazy;

import me.onethecrazy.util.*;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.objects.save.ClientSkin;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.UniversalParser;
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
    public static Map<String, CacheSkin> skinCache = new HashMap<>();

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void pickClientSkin(){

        // Open File picker dialogue
        ClientFileUtil.modelPickerDialog()
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

                    SkinManager.selectSelfSkin(Path.of(f));
                });
    }

    public static void selectSelfSkin(Path dataPath){
        try{
            String uuid = client.getSession().getUuidOrNull().toString();

            byte[] data3D = FileUtil.read3DDataFile(dataPath);
            String name = dataPath.getFileName().toString();
            String hash = FileUtil.getSha256(data3D);
            ParsingFormat format = UniversalParser.getParsingFormat(dataPath);

            assert format != null;

            FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, format), data3D);

            AllTheSkinsClient.options().selectedSkin = new ClientSkin(hash, name, format);

            // Save the updated options:
            FileUtil.writeSave(AllTheSkinsClient.options());

            // Reload self skin
            loadSelfSkin();

            // Send update to server
            BackendInteractor.setSkinData(uuid, data3D, format);
        }
        catch(Exception ex){
            AllTheSkins.LOGGER.info("Ran into error while setting self skin: {0}", ex);
        }
    }

    public static void resetSelfSkin(){
        String uuid = client.getSession().getUuidOrNull().toString();

        AllTheSkinsClient.options().selectedSkin = new ClientSkin();

        // Reload self skin
        loadSelfSkin();

        // Save in options
        FileUtil.writeSave(AllTheSkinsClient.options());

        // Send update to server
        BackendInteractor.setSkinData(uuid, new byte[0], ParsingFormat.OBJ);
    }

    public static void loadSelfSkin(){
        String uuid = client.getSession().getUuidOrNull().toString();

        // Set self skin to empty if we don't have a selected skin
        var selectedSkin = AllTheSkinsClient.options().selectedSkin;

        if(Objects.equals(selectedSkin.hash, "")){
            putLookupEntry(uuid, "");
            putCacheEntry(uuid, null, null);

            return;
        }

        // Load self skin
        try{
            Path data3DPath = FileUtil.getSkinPath(selectedSkin.hash, selectedSkin.format);
            List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(data3DPath, selectedSkin.format));

            putLookupEntry(uuid, AllTheSkinsClient.options().selectedSkin.hash);
            putCacheEntry(uuid, vertices, selectedSkin.format);
        }
        catch(Exception e){
            AllTheSkins.LOGGER.info("Ran into error while loading self skin data3D content:", e);
        }
    }

    public static void loadSkin(String uuid){
        // /getSkin
        // -> check local skins
        //      If not in local skins -> /files
        //          If response empty -> set entry to null
        //          Else -> entry to deserialized data3D

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
        // Load from I/O cache
        if(FileUtil.isSkinCached(skinLookup.get(uuid)))
        {
            try {
                Path path = FileUtil.tryGetSkinFromIOCache(skinLookup.get(uuid));

                // If path == null something went horribly wrong
                assert path != null;

                List<Vertex> vertices = ModelNormalizer.normalize(
                        UniversalParser.parse(
                            path
                        )
                );

                putCacheEntry(uuid, vertices, UniversalParser.getParsingFormat(path));
            } catch (Exception e) {
                putCacheEntry(uuid, null, null);

                AllTheSkins.LOGGER.error("Ran into error while loading skin from I/O Cache: {0}", e);
            }
        }
        // Request from Server
        else{
            BackendInteractor.getSkinData(skinLookup.get(uuid), (data3D, format) -> {
                if(data3D.length != 0){
                    String hash = skinLookup.get(uuid);

                    // Try saving to local Cache
                    try {
                        FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, format), data3D);
                    } catch (IOException e) {
                        AllTheSkins.LOGGER.error("Ran into error while saving skin to i/o cache: {0}", e);
                    }

                    List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(FileUtil.getSkinPath(hash, format)));
                    putCacheEntry(uuid, vertices, format);
                }
                else{
                    putCacheEntry(uuid, null, null);
                }
            });
        }
    }

    public static void putCacheEntry(String uuid, @Nullable List<Vertex> vertices, ParsingFormat format){
        skinCache.put(uuid, new CacheSkin(vertices, format));
    }

    public static void putLookupEntry(String uuid, @Nullable String id){
        skinLookup.put(uuid, id);
    }
}
