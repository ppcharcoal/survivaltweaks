package com.survivaltweaks.mixin;

import com.survivaltweaks.AnvilRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mod's only mixin. Deliberately minimal - all logic lives in AnvilRules.
 *
 * Hooked vanilla methods (names must match Minecraft's own method names):
 *  - createResult()                    : name confirmed by Fabric API's own 26.3 AnvilMenuMixin
 *  - mayPickup(Player, boolean)        : vanilla's "may the player take the result" check
 *
 * survivaltweaks.mixins.json is "required": false. If an update renames either
 * method, the log shows a mixin error for this class and the anvil works like
 * vanilla; nothing else in the mod is affected.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Inject(method = "createResult", at = @At("TAIL"))
    private void survivaltweaks$afterCreateResult(CallbackInfo ci) {
        AnvilRules.afterCreateResult((AnvilMenu) (Object) this);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void survivaltweaks$allowFreePickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        if (hasStack && AnvilRules.isFree((AnvilMenu) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
