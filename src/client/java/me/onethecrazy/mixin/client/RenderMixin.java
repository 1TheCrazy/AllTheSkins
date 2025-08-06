package me.onethecrazy.mixin.client;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public class RenderMixin <T extends LivingEntity, S extends LivingEntityRenderState>{
    private AbstractClientPlayerEntity player;

    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(LivingEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(state instanceof PlayerEntityRenderState playerState){
            String uuid = player.getUuid().toString();

            // We have never encountered this user before (we don't know whether he has a skin or not) or we have never loaded the skin of this user
            if(!SkinManager.skinLookup.containsKey(uuid) || !SkinManager.skinCache.containsKey(uuid)){
                AllTheSkins.LOGGER.info("Loading skin for uuid: {}", uuid);
                SkinManager.loadSkin(uuid);
                return;
            }

            @Nullable List<Vertex> vertices = SkinManager.skinCache.get(uuid);

            // User didn't select a skin
            if(vertices == null || vertices.isEmpty())
                return;

            matrixStack.push();

            // Apply player Yaw
            float rot = playerState.relativeHeadYaw + state.bodyYaw;
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));

            // Get Matrices
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f matrix = entry.getPositionMatrix();

            RenderLayer layer = RenderLayer.getEntityCutoutNoCull(Identifier.of(AllTheSkins.MOD_ID, "textures/white_pixel.png"));
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(layer);

            for(Vertex v : vertices){
                buffer.vertex(matrix, v.position.x, v.position.y, v.position.z).color(1f, 1f, 1f, 1f).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, v.normals.x, v.normals.y, v.normals.z);
            }

            matrixStack.pop();

            ci.cancel();
        }
    }

    // updateRenderState is called every frame BEFORE render, so we're guaranteed to have a value in player
    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at=@At("HEAD"))
    private void onUpdateRenderState(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci){
        if(livingEntity instanceof AbstractClientPlayerEntity)
            player = (AbstractClientPlayerEntity) livingEntity;
    }
}
