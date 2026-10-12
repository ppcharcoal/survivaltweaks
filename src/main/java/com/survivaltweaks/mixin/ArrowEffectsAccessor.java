package com.survivaltweaks.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets PotionTweaks call vanilla's own Arrow.doPostHurtEffects(LivingEntity) - the
 * method that applies a tipped arrow's effects (with vanilla's 1/8 duration).
 * Verified against the 26.3 jar: protected void doPostHurtEffects(LivingEntity).
 * If a future version renames it, this accessor isn't applied and PotionTweaks
 * skips the dragon case (it checks "instanceof ArrowEffectsAccessor" first).
 */
@Mixin(Arrow.class)
public interface ArrowEffectsAccessor {
    @Invoker("doPostHurtEffects")
    void survivaltweaks$doPostHurtEffects(LivingEntity target);
}
