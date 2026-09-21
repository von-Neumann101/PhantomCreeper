package com.mestery.phantomcreeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = PhantomCreeperMod.MOD_ID)
public final class PhantomCreeperSpawning {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceNaturalPhantom(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() != EntityType.PHANTOM || event.getSpawnType() != MobSpawnType.NATURAL
                || event.isSpawnCancelled() || !(event.getLevel() instanceof ServerLevel level)
                || event.getEntity().getRandom().nextInt(4) != 0) {
            return;
        }
        PhantomCreeper replacement = PhantomCreeperMod.PHANTOM_CREEPER.get().create(level);
        if (replacement != null) {
            replacement.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                    event.getEntity().getYRot(), event.getEntity().getXRot());
            EventHooks.finalizeMobSpawn(replacement, level, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
            if (level.addFreshEntity(replacement)) {
                event.setSpawnCancelled(true);
            }
        }
    }
}
