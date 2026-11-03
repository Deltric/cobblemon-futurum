/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.layer

import com.cobblemon.mod.common.client.render.AvatarPokemonState
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.player.RecallArmAnimationTracker
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

class PlayerRecallBallRenderer(
    renderLayerParent: RenderLayerParent<AvatarRenderState, PlayerModel>
) : RenderLayer<AvatarRenderState, PlayerModel>(renderLayerParent) {

    override fun submit(matrixStack: PoseStack, collector: SubmitNodeCollector, packedLight: Int, state: AvatarRenderState, yaw: Float, pitch: Float) {
        val recall = (state as AvatarPokemonState).`cobblemon$getRecallBall`() ?: return
        val geometry = recall.geometry ?: return

        matrixStack.pushPose()
        // The parent model is posed for this state (including the raised arm) before its layers submit.
        parentModel.translateToHand(state, HumanoidArm.RIGHT, matrixStack)
        recall.beamLocal?.let { beam ->
            val offset = Matrix4f(matrixStack.last().pose()).mul(beam).transformPosition(Vector3f(0F, 0.18F, -0.3F))
            recall.anim.beamOrigin = Minecraft.getInstance().gameRenderer.mainCamera().position().add(Vec3(offset))
        }
        geometry.submit(matrixStack, collector, state.outlineColor)
        matrixStack.popPose()
    }

    companion object {
        fun extract(player: Player, state: AvatarRenderState, partialTicks: Float): RecallArmAnimationTracker.RecallBallRender? {
            val anim = RecallArmAnimationTracker.get(player.uuid) ?: return null
            // Riders keep their mounted pose.
            val raise = if (player.vehicle is PokemonEntity) 0F else RecallArmAnimationTracker.raiseFactor(player.uuid)
            val mc = Minecraft.getInstance()
            if (player === mc.player && mc.options.cameraType.isFirstPerson) {
                return RecallArmAnimationTracker.RecallBallRender(anim, raise, null, null)
            }

            val pose = PoseStack()
            pose.rotate(Axis.XP.rotationDegrees(-90F))
            pose.rotate(Axis.YP.rotationDegrees(180F))
            pose.translate(0.0625, 0.125, -0.625)
            pose.scale(0.54F, -0.54F, -0.54F)
            pose.translate(0.0, 0.5, 0.0)
            pose.rotate(Axis.YP.rotationDegrees(180F))

            val builder = ModelGeometry.Builder()
            RecallArmAnimationTracker.extractBall(anim, pose, builder, state.lightCoords, partialTicks)
            val beamLocal = anim.ballState.locatorStates["beam"]?.let { Matrix4f(pose.last().pose()).mul(it.matrix) }
            return RecallArmAnimationTracker.RecallBallRender(anim, raise, builder.build(), beamLocal)
        }
    }
}
