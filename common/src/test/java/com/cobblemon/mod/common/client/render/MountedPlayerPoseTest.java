/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MountedPlayerPoseTest {
    private HumanoidModel<HumanoidRenderState> model() {
        Map<String, ModelPart> parts = new HashMap<>();
        for (String name : List.of("body", "right_arm", "left_arm", "right_leg", "left_leg")) {
            parts.put(name, new ModelPart(List.of(), Map.of()));
        }
        parts.get("left_arm").setInitialPose(PartPose.offset(5, 2, 0));
        parts.put("head", new ModelPart(List.of(), Map.of("hat", new ModelPart(List.of(), Map.of()))));
        return new HumanoidModel<>(new ModelPart(List.of(), parts));
    }

    @Test void riderSnapshotsAreIndependentAndKeepEachModelsRestOffsets() {
        Map<String, PartPose> animation = new HashMap<>();
        animation.put("arm_left", PartPose.offsetAndRotation(1, 3, 0, -1, 0, 0));
        animation.put("body", PartPose.offset(2, 10, 0));
        var first = new MountedPlayerPose(animation);
        animation.put("arm_left", PartPose.rotation(0.5f, 0, 0));
        var second = new MountedPlayerPose(animation);
        var model = model();
        first.apply(model);
        assertEquals(6f, model.leftArm.x);
        assertEquals(5f, model.leftArm.y);
        assertEquals(-1f, model.leftArm.xRot);
        assertEquals(-2f, model.root().x);
        assertEquals(0f, model.root().y);
        second.apply(model);
        assertEquals(5f, model.leftArm.x);
        assertEquals(0.5f, model.leftArm.xRot);
        first.apply(model);
        assertEquals(6f, model.leftArm.x);
        assertEquals(-1f, model.leftArm.xRot);
        assertEquals(-2f, model.root().x);
    }
}
