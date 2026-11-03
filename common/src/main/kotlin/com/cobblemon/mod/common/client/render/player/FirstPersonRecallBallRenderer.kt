/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.player

import com.cobblemon.mod.common.client.render.ModelGeometry
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.OverlayTexture

/** Shows the recall ball in place of the local player's main hand while in first person. */
object FirstPersonRecallBallRenderer {
    private var geometry: ModelGeometry? = null

    fun extract(player: LocalPlayer, partialTicks: Float) {
        geometry = null
        val anim = RecallArmAnimationTracker.get(player.uuid) ?: return
        val rise = RecallArmAnimationTracker.raiseFactor(player.uuid)

        val pose = PoseStack()
        pose.translate(0.45, -0.55 - 0.6 * (1.0 - rise), -0.6)
        pose.scale(0.45F, -0.45F, -0.45F)
        pose.rotate(Axis.YP.rotationDegrees(180F))
        pose.rotate(Axis.XP.rotationDegrees(15F))
        val builder = ModelGeometry.Builder()
        // Light is supplied by the hand renderer at submission.
        RecallArmAnimationTracker.extractBall(anim, pose, builder, 0, partialTicks)
        geometry = builder.build()
    }

    /** Submits the ball for this frame, returning false when there is no active recall to show. */
    fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, packedLight: Int): Boolean {
        val geometry = geometry ?: return false
        geometry.submitWithLight(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0)
        return true
    }
}
