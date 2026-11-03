/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/** Immutable animation deltas evaluated while the Pokémon and rider are available. */
public record MountedPlayerPose(Map<String, PartPose> parts) {
    public MountedPlayerPose { parts = Map.copyOf(parts); }

    public void apply(HumanoidModel<?> model) {
        applyPart(model.leftArm, parts.get("arm_left"));
        applyPart(model.rightArm, parts.get("arm_right"));
        applyPart(model.leftLeg, parts.get("leg_left"));
        applyPart(model.rightLeg, parts.get("leg_right"));
        applyPart(model.head, parts.get("head"));
        applyPart(model.hat, parts.get("hat"));
        applyPart(model.body, parts.get("torso"));
        PartPose root = parts.get("body");
        if (root != null) {
            applyPart(model.root(), new PartPose(-root.x(), root.y() - 10f, root.z(),
                root.xRot(), root.yRot(), root.zRot(), root.xScale(), root.yScale(), root.zScale()));
        }
    }

    private static void applyPart(ModelPart part, PartPose delta) {
        if (delta == null) return;
        part.resetPose();
        part.x += delta.x();
        part.y += delta.y();
        part.z += delta.z();
        part.xRot = delta.xRot();
        part.yRot = delta.yRot();
        part.zRot = delta.zRot();
        part.xScale *= delta.xScale();
        part.yScale *= delta.yScale();
        part.zScale *= delta.zScale();
    }
}
