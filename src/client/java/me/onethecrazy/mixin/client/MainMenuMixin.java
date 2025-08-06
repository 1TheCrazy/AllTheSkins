package me.onethecrazy.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.ClientFileUtil;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.FileUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(TitleScreen.class)
public abstract class MainMenuMixin extends Screen{
    private static final int MARGIN = 6;
    private static final int TEXT_OFFSET = 2;
    // To be compatible with the TitleScreen
    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TOP_OFFSET = 48;
    private static final int Y_SPACING = 24;

    public ButtonWidget selectSkinButton;

    protected MainMenuMixin(Text title) {
        super(title);
    }

    // Originally I wanted to draw the skin here, but it's horrendous to get a ClientPlayerEntity without being in a real ClientWorld, so I decided to just not do that :/
    // We also cannot supply the vertices in a good way (bc fuck me ig ¯\_(ツ)_/¯)
    @Inject(method = "init*", at = @At("TAIL"))
    private void onInit(CallbackInfo ci){
        // Add button
        selectSkinButton = ButtonWidget.builder(Text.empty(), (button) -> {
            // Open File picker dialogue
            ClientFileUtil.objPickerDialog()
                    // Execute when user completes File-Selection
                    .thenAccept(f -> {
                        if(f == null || Objects.equals(f, ""))
                            return;

                        SkinManager.selectSelfSkin(f);
                    });
        }).dimensions(this.width / 2 + MARGIN + BUTTON_WIDTH, this.height / 4 + TOP_OFFSET + Y_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        this.addDrawableChild(selectSkinButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci, @Local(ordinal = 1) float f){
        var client = MinecraftClient.getInstance();
        var textRenderer = client.textRenderer;

        Text buttonText = Objects.equals(AllTheSkinsClient.options().selectedSkin.id, "") ? Text.translatable("alltheskins.select_skin") : Text.of(AllTheSkinsClient.options().selectedSkin.name);
        selectSkinButton.setMessage(buttonText);

        ctx.drawText(textRenderer, AllTheSkinsClient.bannerText, this.width / 2 + MARGIN + BUTTON_WIDTH + TEXT_OFFSET, this.height / 4 + TOP_OFFSET + Y_SPACING * 2 + BUTTON_HEIGHT + MARGIN, ColorHelper.withAlpha(f, 0xFFFFFF), true);
    }
}
