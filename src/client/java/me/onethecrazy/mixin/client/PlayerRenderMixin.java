package me.onethecrazy.mixin.client;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.Vertex;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public class PlayerRenderMixin {

    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(LivingEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(state instanceof PlayerEntityRenderState playerState){

            matrixStack.push();
            matrixStack.translate(0, 0, 0);
            matrixStack.scale(0.05f, 0.05f, 0.05f);

            // Apply player Yaw
            float rot = playerState.relativeHeadYaw + state.bodyYaw;
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));

            // Get Matrices
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f matrix = entry.getPositionMatrix();

            RenderLayer layer = RenderLayer.getEntityCutoutNoCull(Identifier.of(AllTheSkins.MOD_ID, "textures/white_pixel.png"));
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(layer);

            float r = 1f, g = 1f, b = 1f, a = 1f;

            if(AllTheSkins.HEAD_MODEL.isPresent()){
                List<Vertex> verticies = AllTheSkins.HEAD_MODEL.get();

                for(Vertex v : verticies){
                    buffer.vertex(matrix, v.position.x, v.position.y, v.position.z).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, v.normals.x, v.normals.y, v.normals.z);
                }
            }

            matrixStack.pop();

            //ci.cancel();
        }

    }
}
