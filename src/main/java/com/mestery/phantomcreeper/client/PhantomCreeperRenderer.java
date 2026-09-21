package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.PhantomCreeper;
import com.mestery.phantomcreeper.PhantomCreeperMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PhantomCreeperRenderer extends MobRenderer<PhantomCreeper, PhantomCreeperModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PhantomCreeperMod.MOD_ID, "textures/entity/phantom_creeper.png");

    public PhantomCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomCreeperModel(context.bakeLayer(PhantomCreeperModel.LAYER)), 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomCreeper entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(PhantomCreeper entity, PoseStack pose, float partialTick) {
        float fuse = entity.getFuseProgress(partialTick);
        float swell = 1 + Mth.sin(fuse * 100) * fuse * 0.015F;
        float size = (1 + 0.15F * entity.getPhantomSize()) * (1 + fuse * fuse * fuse * fuse * 0.25F) * swell;
        pose.scale(size, size, size);
        pose.translate(0, 1.3125F, 0.1875F);
    }

    @Override
    protected float getWhiteOverlayProgress(PhantomCreeper entity, float partialTick) {
        float fuse = entity.getFuseProgress(partialTick);
        return (int) (fuse * 10) % 2 == 0 ? 0 : Mth.clamp(fuse, 0.5F, 1);
    }

    @Override
    protected void setupRotations(PhantomCreeper entity, PoseStack pose, float age, float yaw, float partialTick, float scale) {
        super.setupRotations(entity, pose, age, yaw, partialTick, scale);
        pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }
}
