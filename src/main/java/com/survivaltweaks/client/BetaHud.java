package com.survivaltweaks.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Beta 1.7-style HUD: the hunger bar is gone and the armor bar sits in its place,
 * on the right side above the hotbar (filling right-to-left like the old one).
 *
 * Built on Fabric's HudElementRegistry + HudStatusBarHeightRegistry (no mixins),
 * following Fabric's own HudStatusBarHeightsTest example for 26.3:
 *  - FOOD_BAR element is replaced by our armor drawing, at the food bar position.
 *    Its height is 10 px only when you have armor, so air bubbles sit right above it
 *    (or drop down to the hotbar row when you have no armor).
 *  - ARMOR_BAR element (left side) is emptied and its height set to 0.
 *  - Exception: while riding something with hearts (horse, pig, strider...), vanilla
 *    shows the mount's hearts on the right, so armor falls back to its normal place
 *    on the left to avoid overlapping.
 */
public final class BetaHud implements ClientModInitializer {
    private static final Identifier ARMOR_EMPTY = Identifier.withDefaultNamespace("hud/armor_empty");

    /**
     * Column where the filled part of a half icon starts (0-8). 4 = the right half
     * including the middle column, mirroring vanilla's left-filled half icon.
     * Use 5 if the half looks one pixel too wide.
     */
    private static final int HALF_SPLIT = 4;
    private static final Identifier ARMOR_FULL = Identifier.withDefaultNamespace("hud/armor_full");

    @Override
    public void onInitializeClient() {
        // Right side: the food bar slot now shows armor.
        HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR,
                (HudElement vanillaFood) -> BetaHud::extractRightArmor);
        HudStatusBarHeightRegistry.addRight(VanillaHudElements.FOOD_BAR,
                (Player player) -> showsBars() && !isRidingMountWithHearts(player) && player.getArmorValue() > 0 ? 10 : 0);

        // Left side: vanilla armor bar only while riding (see class comment).
        HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR,
                (HudElement vanillaArmor) -> (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> {
                    Player player = Minecraft.getInstance().player;
                    if (player != null && isRidingMountWithHearts(player)) {
                        vanillaArmor.extractRenderState(graphics, deltaTracker);
                    }
                });
        HudStatusBarHeightRegistry.addLeft(VanillaHudElements.ARMOR_BAR,
                (Player player) -> isRidingMountWithHearts(player) && player.getArmorValue() > 0 ? 10 : 0);
    }

    private static void extractRightArmor(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !showsBars() || isRidingMountWithHearts(player)) {
            return;
        }

        int armor = player.getArmorValue();
        if (armor <= 0) {
            return;
        }

        int y = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR);
        int right = graphics.guiWidth() / 2 + 91;

        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            if (i * 2 + 1 < armor) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL, x, y, 9, 9);
            } else if (i * 2 + 1 == armor) {
                drawMirroredHalf(graphics, x, y);
            } else {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY, x, y, 9, 9);
            }
        }
    }

    /**
     * Vanilla's armor_half sprite is filled on the LEFT (vanilla fills left-to-right).
     * This bar fills right-to-left, so the half icon must be filled on the RIGHT:
     * draw the empty icon, then the full icon clipped (scissor) to its right part.
     * Uses only blitSprite + enableScissor/disableScissor (both confirmed in 26.3),
     * no texture flipping and no extra texture files.
     */
    private static void drawMirroredHalf(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY, x, y, 9, 9);
        graphics.enableScissor(x + HALF_SPLIT, y, x + 9, y + 9);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL, x, y, 9, 9);
        graphics.disableScissor();
    }

    /** Survival/adventure only - same condition vanilla uses for health/armor/food. */
    private static boolean showsBars() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer();
    }

    private static boolean isRidingMountWithHearts(Player player) {
        return player.getVehicle() instanceof LivingEntity;
    }
}
