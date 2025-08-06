package me.onethecrazy.util;


import com.google.gson.Gson;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.objects.save.AllTheSkinsSave;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtil {
    public static String readJSONObjectFileContents(Path file) throws IOException{
        createFileIfNotPresent(file, "{}");

        return Files.readString(file);
    }

    public static String readOBJFile(Path file) throws IOException{
        createFileIfNotPresent(file, "");

        return Files.readString(file);
    }

    public static void writeFile(Path file, String content) throws IOException{
        createFileIfNotPresent(file, "");

        Files.writeString(file, content);
    }

    public static AllTheSkinsSave loadSave() throws IOException {
        String saveContents = readJSONObjectFileContents(getSavePath());

        Gson gson = new Gson();

        return gson.fromJson(saveContents, AllTheSkinsSave.class);
    }

    public static void createFileIfNotPresent(Path file, String emptyFileContent) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        try {
            Files.createFile(file);
            // Write Empty json, so we don't get an exception when reading the file contents and just parsing them via gson
            Files.writeString(file, emptyFileContent);
        }
        // File already exists
        catch(FileAlreadyExistsException e){ }
    }

    public static void createPaths() {
        try{
            Files.createDirectory(getDefaultPath());
            createFileIfNotPresent(getSavePath(), "{}");
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

    public static Path getSkinPath(String skin){
        return getSkinsPath().resolve(skin + ".obj");
    }

    public static String getSha256(String input){
        try {
            // Get a SHA-256 MessageDigest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Compute the hash as bytes
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
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
        Path filePath = getSkinPath(hash);
        return doesFileExist(filePath);
    }

    public static boolean doesFileExist(Path filePath){
        return Files.exists(filePath);
    }

    public static void writeSave(AllTheSkinsSave save) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(save);

        writeFile(getSavePath(), json);
    }
}
