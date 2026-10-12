package com.survivaltweaks;

import com.survivaltweaks.mixin.ArrowEffectsAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Tipped arrows on the Ender Dragon.
 *
 * Arrows hit the dragon's body PARTS, which are not living entities, so vanilla
 * never runs the arrow's effect code for the dragon. But all part damage is
 * passed on to the dragon itself, where Fabric's AFTER_DAMAGE event fires with
 * the arrow as the direct damage source. At that point we run the arrow's own
 * vanilla effect method on the dragon (same effects, same 1/8 duration).
 * The dragon accepting effects at all is handled by mixin/BossEffectsMixin.
 *
 * Everything else about potions on mobs is done by the mixins (see MAINTAINING.md 3.15).
 */
public final class PotionTweaks {
    private PotionTweaks() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!blocked
                    && entity instanceof EnderDragon dragon
                    && source.getDirectEntity() instanceof ArrowEffectsAccessor arrow) {
                arrow.survivaltweaks$doPostHurtEffects(dragon);
            }
        });
    }
}
