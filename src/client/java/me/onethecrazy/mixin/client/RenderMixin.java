package me.onethecrazy.mixin.client;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderMixin <T extends LivingEntity, S extends LivingEntityRenderState> implements LivingEntityRenderExtension {
    @Unique private AbstractClientPlayerEntity player;

    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(S state, MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci){
        // We only want to hook the player rendering
        if(state instanceof PlayerEntityRenderState playerState){
            if(!AllTheSkinsClient.options().isEnabled)
                return;

            String uuid;

            try {
                uuid = player.getUuid().toString();
            }
            catch(NullPointerException ex){
                var screen = MinecraftClient.getInstance().currentScreen;

                // We are inside a screen and don't have an uuid, so we just fall back to the clients uuid
                if(screen instanceof TitleScreen || screen instanceof ConfigScreen)
                    uuid = MinecraftClient.getInstance().getSession().getUuidOrNull().toString();
                // Just hand off to default rendering
                else
                    return;
            }

            // We have never encountered this user before (we don't know whether he has a skin or not) or we have never loaded the skin of this user
            if(!SkinManager.skinLookup.containsKey(uuid)){
                AllTheSkins.LOGGER.info("Loading skin for uuid: {}", uuid);
                SkinManager.loadSkin(uuid);
                return;
            }

            @Nullable CacheSkin cacheResult = SkinManager.skinCache.get(uuid);

            // We don't have the skin data yet
            if(cacheResult == null)
                return;

            @Nullable List<Vertex> vertices = cacheResult.vertices;

            // User didn't select a skin
            if(vertices == null || vertices.isEmpty())
                return;

            matrixStack.push();

            // --- Stolen from net.minecraft.client.render.entity.LivingEntityRenderer#render ---
            if (state.isInPose(EntityPose.SLEEPING)) {
                Direction direction = state.sleepingDirection;
                if (direction != null) {
                    float f = state.standingEyeHeight - 0.1F;
                    matrixStack.translate((float)(-direction.getOffsetX()) * f, 0.0F, (float)(-direction.getOffsetZ()) * f);
                }
            }

            // Render Nametag
            renderNameTagIfShouldRender(state, matrixStack, queue, cameraRenderState);

            // Apply player Yaw
            float rot = playerState.relativeHeadYaw + state.bodyYaw;
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));

            // Get Matrices
            java.util.Map<net.minecraft.util.Identifier, java.util.List<Vertex>> byTex = new java.util.HashMap<>();
            for (Vertex v : vertices) {
                byTex.computeIfAbsent(v.texture, __ -> new java.util.ArrayList<>()).add(v);
            }

            // Use batching queue order 0 (default)
            var batching = queue.getBatchingQueue(0);

            for (var e : byTex.entrySet()) {
                var tex = e.getKey();
                var vertsForTex = e.getValue();

                RenderLayer layer = RenderLayer.getEntityCutoutNoCull(tex);

                batching.submitCustom(matrixStack, layer, (MatrixStack.Entry entry, VertexConsumer vc) -> {
                    Matrix4f posMat = entry.getPositionMatrix();
                    for (Vertex v : vertsForTex) {
                        vc.vertex(posMat, v.position.x, v.position.y, v.position.z)
                                .color(v.color)
                                .texture(v.textureUV.u, v.textureUV.v)
                                .overlay(OverlayTexture.DEFAULT_UV)
                                .light(state.light)
                                .normal(entry, v.normals.x, v.normals.y, v.normals.z);
                    }
                });
            }

            matrixStack.pop();
            ci.cancel();
        }
    }

    @Unique
    // Used to reset the rendered Player, since the flow is as following:
    // Update State (set player for UUID-getting) -> Render immediately -> Same process for other entity
    // To now reset the player we use this method.
    // This is used when rendering the Player Skin Preview.
    public void all_the_skins$setPlayerAsNull(){
        player = null;
    }

    // updateRenderState is called every frame BEFORE render, so we're guaranteed to have a value in player
    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at=@At("HEAD"))
    private void onUpdateRenderState(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci){
        if(livingEntity instanceof AbstractClientPlayerEntity)
            player = (AbstractClientPlayerEntity) livingEntity;
    }

    // --- Stolen and modified from net.minecraft.client.render.entity.EntityRenderer#renderLabelIfPresent ---
    @Unique
    private void renderNameTagIfShouldRender(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState){
        if (state.displayName != null)
            queue.submitLabel(matrices, state.nameLabelPos, 0, state.displayName, !state.sneaking, state.light, state.squaredDistanceToCamera, cameraRenderState);
    }
}
