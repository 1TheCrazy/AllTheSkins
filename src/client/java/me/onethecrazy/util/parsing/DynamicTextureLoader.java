package me.onethecrazy.util.parsing;

import com.mojang.blaze3d.systems.RenderSystem;
import me.onethecrazy.AllTheSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

// This is FULLY vibe coded
// This texture shit is a PAIN
public final class DynamicTextureLoader {

    private DynamicTextureLoader() {}

    public static Identifier load(Path imagePath) throws Exception {
        try (InputStream in = Files.newInputStream(imagePath)) {
            return readDecodeAndRegister(in);
        }
    }

    public static Identifier load(byte[] imageBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(imageBytes)) {
            return readDecodeAndRegister(in);
        }
    }

    private static Identifier readDecodeAndRegister(InputStream in) throws Exception {
        NativeImage image;
        try {
            image = NativeImage.read(in);
        } catch (Exception pngFail) {
            byte[] raw = (in instanceof ByteArrayInputStream)
                    ? ((ByteArrayInputStream) in).readAllBytes()
                    : readAll(in);

            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(raw));
            if (buffered == null) throw pngFail;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "PNG", baos);
            image = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
        }
        return uploadOnRenderThread(image);
    }

    private static Identifier uploadOnRenderThread(NativeImage image) throws Exception {
        final Identifier id = Identifier.of(AllTheSkins.MOD_ID, "dynamic/" + UUID.randomUUID());
        final MinecraftClient mc = MinecraftClient.getInstance();

        if (RenderSystem.isOnRenderThread()) {
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "gltf-dynamic", image);
            mc.getTextureManager().registerTexture(id, tex);
            return id;
        }

        try {
            return mc.submit(() -> {
                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "gltf-dynamic", image);
                mc.getTextureManager().registerTexture(id, tex);
                return id;
            }).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw new RuntimeException(cause);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        in.transferTo(baos);
        return baos.toByteArray();
    }
}
