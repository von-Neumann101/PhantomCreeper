package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.PhantomCreeperMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PhantomCreeperMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PhantomCreeperClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PhantomCreeperMod.PHANTOM_CREEPER.get(), PhantomCreeperRenderer::new);
        event.registerEntityRenderer(PhantomCreeperMod.CALAMITY_PHANTOM.get(), CalamityPhantomRenderer::new);
        event.registerEntityRenderer(PhantomCreeperMod.TRIDENT_ZOMBIE.get(), TridentZombieRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PhantomCreeperModel.LAYER, PhantomCreeperModel::createBodyLayer);
    }
}
