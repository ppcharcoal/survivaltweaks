package com.survivaltweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Potion effects (drunk, splash, lingering, tipped arrows...) work on ALL mobs.
 *
 * Vanilla asks canBeAffected(effect) before adding an effect; mobs answer "no"
 * for their immunities (undead: poison/regeneration, spiders: poison, wither
 * skeletons: wither, entity-type immunity tags...). Two hooks turn that "no" into
 * "yes" for every non-player mob:
 *  1. the canBeAffected call inside addEffect is wrapped - this also overrides
 *     mob classes that answer "no" in their own canBeAffected;
 *  2. LivingEntity.canBeAffected's own answer is changed (tag-based immunities,
 *     and every other place that asks). Signatures confirmed by Fabric API's own 26.3 LivingEntityMixin
 * (fabric-entity-events-v1, effect package).
 *
 * Not changed: undead are still HEALED by Instant Damage and HURT by Instant
 * Health - that is an inversion, not an immunity.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectImmunityMixin {
    @WrapOperation(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z")
    )
    private boolean survivaltweaks$ignoreImmunityOnAdd(LivingEntity self, MobEffectInstance effect, Operation<Boolean> original) {
        return original.call(self, effect) || !(self instanceof Player);
    }

    /**
     * Covers LivingEntity's own immunity rules (entity-type tags, e.g. undead vs
     * poison/regeneration) everywhere canBeAffected is asked - including
     * forceAddEffect, which the boss mixin uses. Done as a return-value tweak
     * (not by wrapping the call inside forceAddEffect) because Fabric API injects
     * at that exact call; a return-value tweak chains safely with Fabric's hook.
     */
    @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
    private boolean survivaltweaks$ignoreImmunity(boolean original) {
        return original || !((Object) this instanceof Player);
    }
}
