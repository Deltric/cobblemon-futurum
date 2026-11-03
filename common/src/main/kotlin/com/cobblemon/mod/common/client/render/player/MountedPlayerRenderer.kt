/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.player

import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.api.riding.RidingAnimation
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.MountedPlayerPose
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.player.Player

/** Evaluates MoLang once during extraction, never from deferred model submission. */
object MountedPlayerRenderer {
    private val defaultAnimations = listOf(RidingAnimation("player", "animation.player.default_ride_pose"))

    fun extract(player: Player, renderState: AvatarRenderState): MountedPlayerPose? {
        val pokemonEntity = player.vehicle as? PokemonEntity ?: return null
        val relevantPartsByName = listOf("body", "arm_left", "arm_right", "leg_left", "leg_right", "head", "hat", "torso")
            .associateWith { ModelPart(emptyList(), emptyMap()) }
        val rotateHead = pokemonEntity.ifRidingAvailableSupply(false) { behaviour, settings, state ->
            behaviour.shouldRotateRiderHead(settings, state, pokemonEntity)
        }
        val headYaw = if (rotateHead) renderState.yRot else 0f
        val headPitch = if (rotateHead) renderState.xRot else 0f
        val ageInTicks = renderState.ageInTicks
        val limbSwing = renderState.walkAnimationPos
        val limbSwingAmount = renderState.walkAnimationSpeed
        val seat = pokemonEntity.getSeatForPassenger(player) ?: return null
        val animations = seat.poseAnimations
            ?.firstOrNull { it.poseTypes.isEmpty() || it.poseTypes.contains(pokemonEntity.getCurrentPoseType()) }?.animations
            ?: defaultAnimations
        val state = pokemonEntity.delegate as PokemonClientDelegate

        relevantPartsByName.values.forEach {
            it.xRot = 0F
            it.yRot = 0F
            it.zRot = 0F
        }

        state.runtime.environment.setSimpleVariable("age_in_ticks", DoubleValue(ageInTicks.toDouble()))
        state.runtime.environment.setSimpleVariable("limb_swing", DoubleValue(limbSwing.toDouble()))
        state.runtime.environment.setSimpleVariable("limb_swing_amount", DoubleValue(limbSwingAmount.toDouble()))
        state.runtime.environment.setSimpleVariable("head_yaw", DoubleValue(headYaw.toDouble()))
        state.runtime.environment.setSimpleVariable("head_pitch", DoubleValue(headPitch.toDouble()))

        // TODO add the q.player reference, requires a cached thing or something on the client idk

        for (animation in animations) {
            val bedrockAnimation = BedrockAnimationRepository.getAnimationOrNull(animation.fileName, animation.animationName) ?: continue
            bedrockAnimation.run(
                context = null,
                relevantPartsByName = relevantPartsByName,
                rootPart = null,
                state = state,
                animationSeconds = state.animationSeconds,
                limbSwing = limbSwing,
                limbSwingAmount = limbSwingAmount,
                ageInTicks = ageInTicks,
                intensity = 1F
            )
        }

        return MountedPlayerPose(relevantPartsByName.mapValues { it.value.storePose() })
    }
}
