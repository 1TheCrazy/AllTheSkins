package me.onethecrazy.util;


import com.google.gson.Gson;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.objects.save.AllTheSkinsSave;
import me.onethecrazy.util.parsing.ParsingFormat;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static String readJSONObjectFileContents(Path file) throws IOException{
        createFileIfNotPresent(file, "{}".getBytes(StandardCharsets.UTF_8));

        return Files.readString(file);
    }

    public static byte[] read3DDataFile(Path file) throws IOException{
        createFileIfNotPresent(file, new byte[0]);

        return Files.readAllBytes(file);
    }

    public static void writeFile(Path file, String content) throws IOException{
        createFileIfNotPresent(file, new byte[0]);

        Files.writeString(file, content);
    }

    public static AllTheSkinsSave loadSave() throws IOException {
        String saveContents = readJSONObjectFileContents(getSavePath());

        Gson gson = new Gson();

        return gson.fromJson(saveContents, AllTheSkinsSave.class);
    }

    public static void createFileIfNotPresent(Path file, byte[] emptyFileContent) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        try {
            Files.createFile(file);
            // Write Empty json, so we don't get an exception when reading the file contents and just parsing them via gson
            Files.write(file, emptyFileContent);
        }
        // File already exists
        catch(FileAlreadyExistsException e){ }
    }

    public static void createPaths() {
        try{
            Files.createDirectory(getDefaultPath());
            createFileIfNotPresent(getSavePath(), "{}".getBytes(StandardCharsets.UTF_8));
            Files.createDirectory(getSkinsPath());
        } catch (IOException e) {
            // Ignore FileAlreadyExistsException
            if(e instanceof FileAlreadyExistsException)
                return;

            AllTheSkins.LOGGER.error("Ran into error while creating default path: {0}", e);
        }
    }

    public static Path getDefaultPath(){
        return FabricLoader.getInstance().getGameDir().resolve(".alltheskins");
    }

    public static Path getSavePath(){
        return getDefaultPath().resolve(".config");
    }

    public static Path getSkinsPath(){
        return getDefaultPath().resolve("skins");
    }

    public static Path getSkinPath(String skin, ParsingFormat format){
        return getSkinsPath().resolve(skin + "." + format.name().toLowerCase());
    }

    public static String getSha256(byte[] input){
        try {
            // Get a SHA-256 MessageDigest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert bytes to hex
            StringBuilder hexString = new StringBuilder(2 * input.length);
            for (byte b : input) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in the Java platform
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static boolean isSkinCached(String hash){
        return tryGetSkinFromIOCache(hash) != null;
    }

    public static @Nullable Path tryGetSkinFromIOCache(String hash){
        // Iterate cached skin files to see if we already got the hash (and therefore the skin)
        try (var stream = Files.list(getSkinsPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().startsWith(hash))
                    .findAny()
                    .orElseThrow();
        } catch (Exception e) {
            // Ignore NoSuchElementException
            if(e instanceof IOException)
                AllTheSkins.LOGGER.error("Failed to iterate local I/O skin cache: ", e);

            // Fallback
            return null;
        }
    }

    public static boolean doesFileExist(Path filePath){
        return Files.exists(filePath);
    }

    public static void writeSave(AllTheSkinsSave save){
        try{
            Gson gson = new Gson();
            String json = gson.toJson(save);

            writeFile(getSavePath(), json);
        }
        catch(Exception ex){
            AllTheSkins.LOGGER.error("Failed to save config: ", ex);
        }
    }
}
