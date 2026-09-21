package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.PhantomCreeper;
import com.mestery.phantomcreeper.PhantomCreeperMod;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Custom geometry with vanilla PhantomModel's wing and tail animation. */
public final class PhantomCreeperModel extends PhantomModel<PhantomCreeper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(PhantomCreeperMod.MOD_ID, "phantom_creeper"), "main");

    public PhantomCreeperModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4, -2, -8, 8, 4, 16),
                PartPose.rotation(-0.08F, 0, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -4, -8, 8, 8, 8), PartPose.offset(0, 0, -8));
        PartDefinition tail = body.addOrReplaceChild("tail_base", CubeListBuilder.create().texOffs(64, 32)
                .addBox(-2, 0, 0, 4, 2, 8), PartPose.offset(0, -1, 8));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(64, 46)
                .addBox(-1.5F, 0, 0, 3, 1, 8), PartPose.offset(0, 0.5F, 8));
        for (boolean left : new boolean[] {true, false}) {
            String side = left ? "left" : "right";
            PartDefinition wing = body.addOrReplaceChild(side + "_wing_base",
                    CubeListBuilder.create().texOffs(0, 32).mirror(!left)
                            .addBox(left ? 0 : -8, 0, 0, 8, 1, 12),
                    PartPose.offset(left ? 4 : -4, -1, -6));
            wing.addOrReplaceChild(side + "_wing_tip", CubeListBuilder.create().mirror(!left)
                            .texOffs(0, 52).addBox(left ? 0 : -4, 0, 0, 4, 1, 12)
                            .texOffs(0, 68).addBox(left ? 4 : -8, 0, 0, 4, 1, 10)
                            .texOffs(0, 84).addBox(left ? 8 : -12, 0, 0, 4, 1, 8),
                    PartPose.offset(left ? 8 : -8, 0, 0));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }
}
