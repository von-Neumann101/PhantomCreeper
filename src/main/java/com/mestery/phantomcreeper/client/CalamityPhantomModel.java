package com.mestery.phantomcreeper.client;

import com.mestery.phantomcreeper.CalamityPhantom;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelPart;

/** Keeps the wings and tail; the renderer supplies the charged Creeper torso and composite heads. */
public final class CalamityPhantomModel extends PhantomModel<CalamityPhantom> {
    private final ModelPart body;

    public CalamityPhantomModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        body.skipDraw = true;
        body.getChild("head").visible = false;
    }

    void translateToBody(PoseStack pose) {
        body.translateAndRotate(pose);
    }

    void translateToWingTip(PoseStack pose, boolean left) {
        String side = left ? "left" : "right";
        ModelPart wing = body.getChild(side + "_wing_base");
        wing.translateAndRotate(pose);
        wing.getChild(side + "_wing_tip").translateAndRotate(pose);
    }
}
