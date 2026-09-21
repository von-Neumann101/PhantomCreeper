package com.mestery.phantomcreeper;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(PhantomCreeperMod.MOD_ID)
public final class PhantomCreeperMod {
    public static final String MOD_ID = "phantomcreeper";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomCreeper>> PHANTOM_CREEPER =
            ENTITIES.register("phantom_creeper", () -> EntityType.Builder.of(PhantomCreeper::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.6F).eyeHeight(0.3F).clientTrackingRange(8)
                    .build(MOD_ID + ":phantom_creeper"));
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("phantom_creeper_spawn_egg",
            () -> new DeferredSpawnEggItem(PHANTOM_CREEPER, 0x429B35, 0x465D9C, new Item.Properties()));

    public PhantomCreeperMod(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(PhantomCreeperMod::attributes);
        bus.addListener(PhantomCreeperMod::creativeTab);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(PHANTOM_CREEPER.get(), Monster.createMonsterAttributes().build());
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(SPAWN_EGG);
        }
    }
}
