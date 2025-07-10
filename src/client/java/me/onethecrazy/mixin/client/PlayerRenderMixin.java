package me.onethecrazy.mixin.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.Identifier;

@Mixin(LivingEntityRenderer.class)
public class PlayerRenderMixin {

    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(LivingEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(state instanceof PlayerEntityRenderState playerState){

            matrixStack.push();
            matrixStack.translate(0, 0, 0);
            matrixStack.scale(1f, 1f, 1f);

            // Apply player Yaw
            float rot = playerState.relativeHeadYaw + state.bodyYaw;
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));

            // Get Matrices
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f matrix = entry.getPositionMatrix();

            RenderLayer layer = RenderLayer.getDebugQuads();
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(layer);

            float r = 1f, g = 1f, b = 1f, a = 1f;
            float min = -0.5f;
            float max = 0.5f;

            buffer.vertex(matrix, min, min, max).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
            buffer.vertex(matrix, max, min, max).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
            buffer.vertex(matrix, max, max, max).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);
            buffer.vertex(matrix, min, max, max).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, 1);

            matrixStack.pop();

            //ci.cancel();
        }

    }
}
