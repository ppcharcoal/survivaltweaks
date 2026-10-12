package com.survivaltweaks.mixin;

import com.survivaltweaks.KeepInventory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Partial keep-inventory hook. Player.dropEquipment(ServerLevel) is the vanilla
 * method that drops a dead player's inventory (verified in the 26.3 jar). At its
 * start, KeepInventory takes the kept items out so vanilla doesn't drop them.
 * Non-required: if this fails to apply, deaths drop everything like vanilla.
 */
@Mixin(Player.class)
public abstract class PlayerDropEquipmentMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void survivaltweaks$keepEquippedAndHotbar(ServerLevel level, CallbackInfo ci) {
        KeepInventory.beforeDrops((Player) (Object) this);
    }
}
