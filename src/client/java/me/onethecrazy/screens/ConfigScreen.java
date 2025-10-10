package me.onethecrazy.screens;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class ConfigScreen extends Screen {
    // Constants
    private static final int SKIN_PREVIEW_DIMENSIONS = 300;
    private static final int MARGIN = 6;
    private static final float SKIN_PREVIEW_SCALE = 140f;
    private static final int BUTTON_WIDTH = 98;
    private static final int SMALL_BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TOP_OFFSET = 48;
    private static final int Y_SPACING = 24;

    // State stuff
    private SkinPreviewRenderer skinPreviewRenderer;
    private ButtonWidget selectSkinButton;
    private ButtonWidget resetButton;
    private ButtonWidget toggleButton;
    private ButtonWidget doneButton;

    public ConfigScreen() {
        super(Text.of("All The Skins"));
    }

    @Override
    protected void init(){
        // init skinPreviewRenderer
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), getScreenFriendlyDimensions(), getScreenFriendlyScale());

        // Init Buttons
        selectSkinButton = ButtonWidget.builder(Text.empty(), (button) -> {
            SkinManager.pickClientSkin();

            // Update Text
            updateSelectButtonText();
        }).dimensions(getCellOriginX(), getCellOriginY() + getScreenFriendlyDimensions() + Y_SPACING, getScreenFriendlyDimensions(), BUTTON_HEIGHT).build();

        resetButton = ButtonWidget.builder(
                    Text.translatable("gui.alltheskins.reset"),
                    (button) -> SkinManager.resetSelfSkin())
                    .dimensions(getCellOriginX(), selectSkinButton.getY() + Y_SPACING, getScreenFriendlyDimensions() / 2 - MARGIN / 2, BUTTON_HEIGHT).build();

        toggleButton = ButtonWidget.builder(Text.empty(), (button) -> {
            AllTheSkinsClient.options().isEnabled = !AllTheSkinsClient.options().isEnabled;

            // Update Text
            updateEnabledButtonText();
        }).dimensions(getCellOriginX() + getScreenFriendlyDimensions() / 2 + MARGIN / 2, selectSkinButton.getY() + Y_SPACING, getScreenFriendlyDimensions() / 2 - MARGIN / 2, BUTTON_HEIGHT).build();

        doneButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                (button) -> close())
                .dimensions(getCellOriginX(), resetButton.getY() + Y_SPACING + 2 * MARGIN, getScreenFriendlyDimensions(), BUTTON_HEIGHT).build();

        this.addDrawableChild(selectSkinButton);
        this.addDrawableChild(resetButton);
        this.addDrawableChild(toggleButton);
        this.addDrawableChild(doneButton);

        // Set Button Texts
        updateSelectButtonText();
        updateEnabledButtonText();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the skin preview
        skinPreviewRenderer.renderPreview(context, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {


        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {


        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {


        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    // -- Save the State --
    @Override
    public void removed() {

    }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }

    // Position Helpers
    @Unique
    private int getCellOriginY(){
        return MARGIN * 4;
    }

    @Unique private int getCellOriginX(){
        return this.width / 2 - getScreenFriendlyDimensions() / 2;
    }

    @Unique private int getScreenFriendlyDimensions(){
        return Math.min(SKIN_PREVIEW_DIMENSIONS, this.height - 150);
    }

    @Unique private float getScreenFriendlyScale(){
        return getScreenFriendlyDimensions() / 2f - 10;
    }

    // Button Text helpers
    @Unique private void updateSelectButtonText(){
        Text text = Objects.equals(AllTheSkinsClient.options().selectedSkin.id, "") ? Text.translatable("gui.alltheskins.select_skin") : Text.of(AllTheSkinsClient.options().selectedSkin.name);

        selectSkinButton.setMessage(text);
    }

    @Unique private void updateEnabledButtonText(){
        Text text = AllTheSkinsClient.options().isEnabled ? Text.translatable("gui.alltheskins.mod_enabled") : Text.translatable("gui.alltheskins.mod_disabled");

        toggleButton.setMessage(text);
    }
}
