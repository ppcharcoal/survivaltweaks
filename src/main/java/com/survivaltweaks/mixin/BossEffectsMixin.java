package com.survivaltweaks.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Ender Dragon and the Wither override addEffect(...) to always refuse every
 * effect. This replaces that refusal with LivingEntity.forceAddEffect(...), the
 * vanilla method that stores an effect (signature confirmed in Fabric API 26.3).
 *
 * Targets are given as class-name STRINGS, so this compiles even if Mojang moves
 * these classes, and each boss is a separate target: if a future version no
 * longer has (or renamed) one of these overrides, the log shows a mixin warning
 * for this class and that boss is simply effect-immune again. Nothing crashes
 * (survivaltweaks.mixins.json is non-required).
 */
@Mixin(targets = {
        "net.minecraft.world.entity.boss.enderdragon.EnderDragon",
        "net.minecraft.world.entity.boss.wither.WitherBoss"
})
public abstract class BossEffectsMixin {
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void survivaltweaks$acceptEffects(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntity) (Object) this).forceAddEffect(effect, source);
        cir.setReturnValue(true);
    }
}
