/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench

import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.entity.PosableEntity
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.resources.Identifier

/**
 * Applies entity-dependent MoLang and animation state during geometry extraction.
 * Deferred render states contain the resulting vertices; this wrapper never travels
 * into Minecraft's render submission queue.
 */
abstract class PosableEntityModel<T : Entity>(
    val renderTypeFunc: (Identifier) -> RenderType = RenderTypes::entityCutoutCull
) {
    val context: RenderContext = RenderContext().also {
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)
    }
    lateinit var posableModel: PosableModel

    fun renderToBuffer(
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val entity = context.request(RenderContext.ENTITY)
        val overlay = getOverlayTexture(entity) ?: packedOverlay
        if (entity is LivingEntity) {
            // We're using the living entity renderer, ergo we need to apply a Y offset of 24 model units (1.5 blocks) for reasons only God can tell us.
            stack.translate(0.0, 1.5, 0.0)
        }
        posableModel.render(context, stack, buffer, packedLight, overlay, color)
        posableModel.setDefault()
    }

    open fun getOverlayTexture(entity: Entity?): Int? {
        return if (entity is LivingEntity) {
            OverlayTexture.pack(
                OverlayTexture.u(0F),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0)
            )
        } else if (entity != null) {
            OverlayTexture.NO_OVERLAY
        } else {
            null
        }
    }

    // Called during extraction while the live entity and its animation state are available.
    fun setupAnim(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        setupEntityTypeContext(entity)
        if (entity is PosableEntity) {
            val ticks = getTicksForAnimation(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
            posableModel.applyAnimations(entity, entity.delegate as PosableState, limbSwing, limbSwingAmount, ticks, headYaw, headPitch)
        }
    }

    /**
     * This is to support cases where the ageInTicks value that should go to the animator is not the same as the entity's
     * normal value. This can happen in the case of riding where it has been rendered in a different part of the pipeline
     * and we want to prevent stuttering / re-render it for a specific moment.
     */
    open fun getTicksForAnimation(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        headYaw: Float,
        headPitch: Float
    ): Float = ageInTicks

    open fun setupEntityTypeContext(entity: Entity?) {
        entity?.let {
            context.put(RenderContext.ENTITY, entity)
            if (it is PosableEntity) {
                context.put(RenderContext.POSABLE_STATE, it.delegate as PosableState)
            }
        }
    }
}
