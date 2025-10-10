package me.onethecrazy.screens.rendering;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinPreviewRenderer {
    private final PlayerEntityRenderState skinPreviewRenderState;
    private final int x, y;
    private final int dimensions;
    private final float scale;

    public SkinPreviewRenderer(int x, int y, int dimensions, float scale){
        var mc = MinecraftClient.getInstance();
        var session = mc.getSession();
        var playerProfile = new GameProfile(session.getUuidOrNull(), session.getUsername());

        // Init render state
        skinPreviewRenderState = new PlayerEntityRenderState();
        skinPreviewRenderState.entityType = EntityType.PLAYER;
        skinPreviewRenderState.squaredDistanceToCamera = 1;
        skinPreviewRenderState.x = skinPreviewRenderState.y = skinPreviewRenderState.z = 0.0;
        skinPreviewRenderState.skinTextures = mc.getSkinProvider().getSkinTextures(playerProfile);

        this.x = x;
        this.y = y;
        this.dimensions = dimensions;
        this.scale = scale;
    }

    public void renderPreview(DrawContext ctx, float deltaTicks){
        // Tick the animation state
        skinPreviewRenderState.age += deltaTicks;

        // Render the border where the Mesh is placed inside
        ctx.drawBorder(x, y, dimensions, dimensions, 0xFFFFFFFF);

        // Render the Preview of the player skin
        ctx.addEntity(
                skinPreviewRenderState,
                scale,
                new Vector3f(0f, 1.0f, 0f),
                new Quaternionf()
                        .rotateAxis(Math.toRadians(180f), 0f, 0f, 1f) // Z correction
                        .rotateAxis(Math.toRadians(180f), 0f, 1f, 0f), // Y correction
                null,
                x, y,
                x + dimensions, y + dimensions
        );
    }

    public void setRotation(){

    }
}
