package me.onethecrazy.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import me.onethecrazy.*;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.ToastUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import org.joml.*;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class MainMenuMixin extends Screen{
    // Constants to be compatible with the TitleScreen
    @Unique private static final int MARGIN = 6;
    @Unique private static final int BUTTON_WIDTH = 98;
    @Unique private static final int Y_SPACING = 24;
    @Unique private static final int SKIN_CELL_DIMENSIONS = 68;

    // Other Constants
    @Unique private static final float PLAYER_SKIN_PREVIEW_SCALE = 30f;

    @Unique private boolean hasModerationNoticeBeenShown = false;
    @Unique private SkinPreviewRenderer skinPreviewRenderer;

    protected MainMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "init*", at = @At("TAIL"))
    private void onInit(CallbackInfo ci){
        // Create a SkinPreviewRenderer instance
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), SKIN_CELL_DIMENSIONS, PLAYER_SKIN_PREVIEW_SCALE);

        // Show Moderation Notice everytime we open Main Menu
        if(!hasModerationNoticeBeenShown && AllTheSkinsClient.isFirstStartup){
            ToastUtil.showModerationNoticeToast();
            hasModerationNoticeBeenShown = true;
        }
    }


    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci, @Local(ordinal = 1) float f){
        // Draw skin Preview
        skinPreviewRenderer.renderPreview(ctx, deltaTicks);
    }

    @Inject(method = "mouseClicked", at=@At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir){
        // If we're inside the skin cell, open config screen
        if(mouseX > getCellOriginX() && mouseX < getCellOriginX() + SKIN_CELL_DIMENSIONS && mouseY > getCellOriginY() && mouseY < getCellOriginY() + SKIN_CELL_DIMENSIONS){
            MinecraftClient.getInstance().setScreen(new ConfigScreen());

            // We handled the click
            cir.setReturnValue(true);
        }
    }

    // Position Helpers
    @Unique private int getCellOriginY(){
        return this.height / 4 + Y_SPACING * 2;
    }

    @Unique private int getCellOriginX(){
        return this.width / 2 - MARGIN - BUTTON_WIDTH - SKIN_CELL_DIMENSIONS;
    }
}
