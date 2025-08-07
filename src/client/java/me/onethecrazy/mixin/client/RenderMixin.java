package me.onethecrazy.mixin.client;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderMixin <T extends LivingEntity, S extends LivingEntityRenderState>{
    private AbstractClientPlayerEntity player;

    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(LivingEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(state instanceof PlayerEntityRenderState playerState){
            if(!AllTheSkinsClient.options().isEnabled)
                return;

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

            // --- Stolen from net.minecraft.client.render.entity.LivingEntityRenderer#render ---
            if (state.isInPose(EntityPose.SLEEPING)) {
                Direction direction = state.sleepingDirection;
                if (direction != null) {
                    float f = state.standingEyeHeight - 0.1F;
                    matrixStack.translate((float)(-direction.getOffsetX()) * f, 0.0F, (float)(-direction.getOffsetZ()) * f);
                }
            }

            // Render Nametag
            renderNameTagIfShouldRender((PlayerEntityRenderState) state, state.displayName, matrixStack, vertexConsumerProvider, light);

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

    // --- Stolen and modified from net.minecraft.client.render.entity.EntityRenderer#renderLabelIfPresent ---
    private void renderNameTagIfShouldRender(PlayerEntityRenderState state,Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light){
        if (state.displayName == null)
            return;


        Vec3d vec3d = state.nameLabelPos;
        if (vec3d != null) {
            boolean bl = !state.sneaking;
            int i = "deadmau5".equals(text.getString()) ? -10 : 0;
            matrices.push();
            matrices.translate(vec3d.x, vec3d.y + 0.5, vec3d.z);
            matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
            matrices.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            float f = (float)(-textRenderer.getWidth((StringVisitable)text)) / 2.0F;
            int j = (int)(MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
            textRenderer.draw(text, f, (float)i, -2130706433, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light);
            if (bl) {
                textRenderer.draw((Text)text, f, (float)i, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
            }

            matrices.pop();
        }
    }
}
