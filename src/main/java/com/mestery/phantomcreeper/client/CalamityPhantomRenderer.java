package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.CalamityPhantom;
import com.mestery.phantomcreeper.PhantomCreeperMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class CalamityPhantomRenderer extends MobRenderer<CalamityPhantom, CalamityPhantomModel> {
    private static final ResourceLocation BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PhantomCreeperMod.MOD_ID, "textures/entity/phantom_creeper.png");

    public CalamityPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new CalamityPhantomModel(context.bakeLayer(PhantomCreeperModel.LAYER)), 1.0F);
        addLayer(new HeadsAndRocketLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(CalamityPhantom entity) {
        return BODY_TEXTURE;
    }

    @Override
    protected void scale(CalamityPhantom entity, PoseStack pose, float partialTick) {
        float fuse = entity.getFuseProgress(partialTick);
        float size = 1 + 0.15F * entity.getPhantomSize() + fuse * fuse * 0.15F;
        pose.scale(size, size, size);
        // Body top is near y + 0.9, matching the real passenger's attachment point.
        pose.translate(0, 0.75F, 0.1875F);
    }

    @Override
    protected float getWhiteOverlayProgress(CalamityPhantom entity, float partialTick) {
        float fuse = entity.getFuseProgress(partialTick);
        return (int) (fuse * 10) % 2 == 0 ? 0 : Mth.clamp(fuse, 0.5F, 1);
    }

    @Override
    protected void setupRotations(CalamityPhantom entity, PoseStack pose, float age, float yaw,
                                  float partialTick, float scale) {
        super.setupRotations(entity, pose, age, yaw, partialTick, scale);
        pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }

    private final class HeadsAndRocketLayer extends RenderLayer<CalamityPhantom, CalamityPhantomModel> {
        private static final ResourceLocation CREEPER = vanillaTexture("creeper/creeper");
        private static final ResourceLocation CREEPER_CHARGE = vanillaTexture("creeper/creeper_armor");
        private static final ResourceLocation WARDEN = vanillaTexture("warden/warden");
        private static final ResourceLocation WARDEN_GLOW = vanillaTexture("warden/warden_bioluminescent_layer");
        private static final ResourceLocation WARDEN_CHARGE = vanillaTexture("warden/warden_pulsating_spots_1");
        private static final ResourceLocation DRAGON = vanillaTexture("enderdragon/dragon");
        private static final ResourceLocation DRAGON_EYES = vanillaTexture("enderdragon/dragon_eyes");
        private static final ResourceLocation WITHER = vanillaTexture("wither/wither");
        private static final ResourceLocation ROCKET = ResourceLocation.withDefaultNamespace("textures/item/firework_rocket.png");
        private final ModelPart wardenHead;
        private final ModelPart creeperBody;
        private final ModelPart chargedBody;
        private final ModelPart witherHead;
        private final DragonHeadModel dragonHead;

        HeadsAndRocketLayer(CalamityPhantomRenderer renderer, EntityRendererProvider.Context context) {
            super(renderer);
            creeperBody = context.bakeLayer(ModelLayers.CREEPER).getChild("body");
            chargedBody = context.bakeLayer(ModelLayers.CREEPER_ARMOR).getChild("body");
            creeperBody.setPos(0, 0, 0);
            chargedBody.setPos(0, 0, 0);
            wardenHead = context.bakeLayer(ModelLayers.WARDEN).getChild("bone").getChild("body").getChild("head");
            wardenHead.setPos(0, 0, 0);
            witherHead = context.bakeLayer(ModelLayers.WITHER).getChild("center_head");
            dragonHead = new DragonHeadModel(context.bakeLayer(ModelLayers.DRAGON_SKULL));
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CalamityPhantom entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float age,
                           float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            int overlay = getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTick));
            pose.pushPose();
            getParentModel().translateToBody(pose);
            renderChargedBody(pose, buffers, light, overlay, age);
            // Move the three touching heads as one cluster so turning cannot open a seam.
            pose.pushPose();
            pose.translate(0, 0.625F, -0.8125F);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.clamp(headYaw, -30, 30)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.clamp(headPitch, -25, 25) * 0.35F));
            pose.translate(0, -0.625F, 0.8125F);
            renderWarden(pose, buffers, light, overlay, entity, age);
            renderDragons(pose, buffers, light, overlay, age);
            pose.popPose();
            renderWingHeads(pose, buffers, light, overlay, age, headYaw, headPitch);
            renderRocket(pose, buffers, light, entity);
            pose.popPose();
        }

        private void renderChargedBody(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age) {
            pose.pushPose();
            pose.translate(0, 0, -0.5F);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            // Lay the vanilla torso along the flight axis, fitting the existing wing and tail joints.
            pose.scale(1, 4.0F / 3.0F, 1);
            creeperBody.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CREEPER)), light, overlay);
            float scroll = age * 0.01F % 1;
            chargedBody.render(pose, buffers.getBuffer(RenderType.energySwirl(CREEPER_CHARGE, scroll, scroll)),
                    light, OverlayTexture.NO_OVERLAY, 0xFF808080);
            pose.popPose();
        }

        private void renderWarden(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                  CalamityPhantom entity, float age) {
            pose.pushPose();
            pose.translate(0, 0.625F, -0.8125F);
            pose.scale(0.78F, 0.78F, 0.78F);
            wardenHead.xRot = 0;
            wardenHead.yRot = 0;
            float tendril = Mth.sin(age * (entity.isChargingSonicBoom() ? 1.8F : 0.15F)) * 0.12F;
            wardenHead.getChild("left_tendril").xRot = tendril;
            wardenHead.getChild("right_tendril").xRot = -tendril;
            wardenHead.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WARDEN)), light, overlay);
            wardenHead.render(pose, buffers.getBuffer(RenderType.eyes(WARDEN_GLOW)), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            if (entity.isChargingSonicBoom()) {
                wardenHead.render(pose, buffers.getBuffer(RenderType.eyes(WARDEN_CHARGE)), LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }

        private void renderDragons(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                   float age) {
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.translate(side * 0.6F, 0.375F, -0.75F);
                pose.scale(0.6F, 0.6F, 0.6F);
                dragonHead.setupAnim(age * 0.25F, 0, 0);
                dragonHead.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DRAGON)), light, overlay);
                dragonHead.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(DRAGON_EYES)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }

        private void renderWingHeads(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                     float age, float yaw, float pitch) {
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                getParentModel().translateToWingTip(pose, side > 0);
                for (int head = 0; head < 3; head++) {
                    pose.pushPose();
                    pose.translate(side * (2 + head * 5) / 16.0F, -0.12F, -0.16F);
                    pose.scale(0.62F, 0.62F, 0.62F);
                    witherHead.yRot = Mth.clamp(yaw, -30, 30) * Mth.DEG_TO_RAD
                            + Mth.sin(age * 0.035F + head) * 0.06F;
                    witherHead.xRot = pitch * Mth.DEG_TO_RAD * 0.25F;
                    witherHead.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WITHER)), light, overlay);
                    pose.popPose();
                }
                pose.popPose();
            }
        }

        private void renderRocket(PoseStack pose, MultiBufferSource buffers, int light, CalamityPhantom entity) {
            pose.pushPose();
            // Mount along the rear body, with the sprite's red nose pointing forward (-Z).
            pose.translate(0, -0.18F, 0.6F);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(1.15F, 1.15F, 1.15F);
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(ROCKET));
            int rocketLight = entity.isRocketBoosting() ? LightTexture.FULL_BRIGHT : light;
            for (int plane = 0; plane < 2; plane++) {
                // Two double-sided flat sprites, crossing along the rocket's long axis.
                rocketVertex(pose.last(), vertices, -0.5F, -0.5F, 0, 0, rocketLight);
                rocketVertex(pose.last(), vertices, -0.5F, 0.5F, 0, 1, rocketLight);
                rocketVertex(pose.last(), vertices, 0.5F, 0.5F, 1, 1, rocketLight);
                rocketVertex(pose.last(), vertices, 0.5F, -0.5F, 1, 0, rocketLight);
                pose.mulPose(Axis.YP.rotationDegrees(90));
            }
            pose.popPose();
        }

        private static void rocketVertex(PoseStack.Pose pose, VertexConsumer vertices,
                                         float x, float y, float u, float v, int light) {
            vertices.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        }

        private static ResourceLocation vanillaTexture(String path) {
            return ResourceLocation.withDefaultNamespace("textures/entity/" + path + ".png");
        }
    }
}
