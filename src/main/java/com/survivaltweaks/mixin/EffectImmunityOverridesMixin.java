package com.survivaltweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mob classes that have their OWN canBeAffected() immunity check (found by scanning
 * the 26.3 jar). Their "no" is turned into "yes", so effects stick everywhere -
 * including forceAddEffect, which the boss mixin uses (the Wither is one of these).
 *
 * Targets are class-name strings: if Mojang removes/renames one of these classes,
 * it is simply skipped. If Mojang ADDS a new class with its own canBeAffected, it
 * is still covered for normal effects by LivingEntityEffectImmunityMixin; add it
 * here too (tools/verify_against_minecraft.py lists all such classes).
 */
@Mixin(targets = {
        "net.minecraft.world.entity.boss.wither.WitherBoss",
        "net.minecraft.world.entity.monster.spider.Spider",
        "net.minecraft.world.entity.monster.skeleton.WitherSkeleton",
        "net.minecraft.world.entity.monster.skeleton.Parched",
        "net.minecraft.world.entity.animal.nautilus.AbstractNautilus"
})
public abstract class EffectImmunityOverridesMixin {
    @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
    private boolean survivaltweaks$ignoreImmunity(boolean original) {
        return true;
    }
}
