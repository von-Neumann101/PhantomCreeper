package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.TridentZombie;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/** Vanilla zombie appearance, baby/riding proportions, and drowned trident arm animation. */
public final class TridentZombieRenderer extends AbstractZombieRenderer<TridentZombie, DrownedModel<TridentZombie>> {
    public TridentZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new RiderModel(context.bakeLayer(ModelLayers.ZOMBIE)),
                new DrownedModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new DrownedModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
    }

    private static final class RiderModel extends DrownedModel<TridentZombie> {
        RiderModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(TridentZombie entity, float limbSwing, float limbSwingAmount, float age,
                              float headYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            if (entity.isPassenger() && entity.getMainHandItem().is(Items.TRIDENT)) {
                boolean rightHand = entity.getMainArm() == HumanoidArm.RIGHT;
                ModelPart arm = rightHand ? rightArm : leftArm;
                float side = rightHand ? 1 : -1;
                // Hold the real equipped spear ahead and beside the large baby head.
                arm.xRot = -0.9F;
                arm.yRot = side * 0.6F;
                arm.zRot = side * 0.1F;
            }
        }
    }
}
