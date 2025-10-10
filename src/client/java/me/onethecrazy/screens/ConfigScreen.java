package me.onethecrazy.screens;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class ConfigScreen extends Screen {
    // Constants
    private static final int SKIN_PREVIEW_DIMENSIONS = 300;
    private static final int MARGIN = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int Y_SPACING = 24;
    private static final float YAW_SENS   = 0.6f;
    private static final float PITCH_SENS = 0.6f;

    // State stuff
    private SkinPreviewRenderer skinPreviewRenderer;
    private ButtonWidget selectSkinButton;
    private ButtonWidget resetButton;
    private ButtonWidget toggleButton;
    private ButtonWidget doneButton;
    private boolean rotating = false;

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
        if (button == 0 && isInsideCell(mouseX, mouseY)) {
            rotating = true;
            return true; // start drag mode
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (rotating && button == 0) {
            rotating = false;
            return true; // stop rotation mode
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rotating && button == 0) {
            // convert mouse motion to yaw/pitch deltas
            float yawDelta   = (float) (deltaX * YAW_SENS);
            float pitchDelta = (float) (-deltaY * PITCH_SENS);

            // apply directly
            skinPreviewRenderer.addRotation(yawDelta, pitchDelta);
            return true; // consume drag
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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

    private boolean isInsideCell(double x, double y) {
        int x0 = getCellOriginX();
        int y0 = getCellOriginY();
        int s  = getScreenFriendlyDimensions();
        return x >= x0 && x <= x0 + s && y >= y0 && y <= y0 + s;
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
