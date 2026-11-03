/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench

import com.cobblemon.mod.common.client.render.ModelGeometry
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose

class PosableLivingRenderState : LivingEntityRenderState() {
    var geometry: ModelGeometry? = null
    var extractedShadowRadius = 0f
}

/**
 * Vanilla extracts movement, visibility, names and lighting; Cobblemon evaluates
 * its entity-dependent animations immediately and freezes the resulting mesh.
 * Neither the extraction adapter nor its model is submitted for drawing.
 */
abstract class PosableLivingEntityRenderer<T : LivingEntity, M : PosableEntityModel<T>>(
    context: EntityRendererProvider.Context,
    val model: M,
    shadow: Float,
    private val mobNameRules: Boolean = false
) : EntityRenderer<T, PosableLivingRenderState>(context) {
    private val vanilla = VanillaExtraction(context)
    private var extracting: PosableLivingRenderState? = null

    init { shadowRadius = shadow }

    abstract fun getTextureLocation(entity: T): Identifier
    protected open fun scale(entity: T, pose: PoseStack, partialTicks: Float) {}
    protected open fun getFlipDegrees(entity: T) = 90f

    override fun createRenderState() = PosableLivingRenderState()

    override fun extractRenderState(entity: T, state: PosableLivingRenderState, partialTicks: Float) {
        vanilla.extractRenderState(entity, state, partialTicks)
        state.geometry = null
        extracting = state
        try {
            vanilla.fallAngle = getFlipDegrees(entity)
            val geometry = ModelGeometry.Builder()
            extractGeometry(entity, entity.getYRot(partialTicks), partialTicks, PoseStack(), geometry, state.lightCoords)
            state.geometry = geometry.build()
            state.extractedShadowRadius = shadowRadius * state.scale * if (mobNameRules) state.ageScale else 1f
        } finally {
            extracting = null
        }
    }

    protected open fun extractGeometry(entity: T, yaw: Float, partialTicks: Float, pose: PoseStack, geometry: ModelGeometry.Builder, light: Int) {
        val state = checkNotNull(extracting)
        pose.pushPose()
        try {
            if (state.hasPose(Pose.SLEEPING)) {
                state.bedOrientation?.let { direction ->
                    val offset = state.eyeHeight - 0.1f
                    pose.translate(-direction.stepX * offset, 0f, -direction.stepZ * offset)
                }
            }
            pose.scale(state.scale, state.scale, state.scale)
            vanilla.applyRotations(state, pose)
            pose.scale(-1f, -1f, 1f)
            scale(entity, pose, partialTicks)
            pose.translate(0f, -1.501f, 0f)
            val transparent = state.isInvisible && !state.isInvisibleToPlayer
            val texture = getTextureLocation(entity)
            val type = when {
                transparent -> RenderTypes.entityTranslucentCull(texture)
                !state.isInvisible -> model.renderTypeFunc(texture)
                state.appearsGlowing() -> RenderTypes.outline(texture)
                else -> null
            }
            if (type != null) {
                model.setupAnim(entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot)
                model.renderToBuffer(pose, geometry.getBuffer(type), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0f), if (transparent) 0x26FFFFFF else -1)
            }
        } finally {
            pose.popPose()
        }
    }

    override fun submit(state: PosableLivingRenderState, pose: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        state.geometry?.submit(pose, collector, state.outlineColor)
        super.submit(state, pose, collector, camera)
    }

    override fun getShadowRadius(state: PosableLivingRenderState) = state.extractedShadowRadius

    override fun shouldShowName(entity: T, distanceToCameraSq: Double): Boolean =
        vanilla.defaultShowName(entity, distanceToCameraSq) &&
            (!mobNameRules || entity.shouldShowName() || entity.hasCustomName() && entity == entityRenderDispatcher.crosshairPickEntity)

    // Required by vanilla's generic extraction class; this model is never drawn.
    private class ExtractionModel : EntityModel<PosableLivingRenderState>(ModelPart(emptyList(), emptyMap()))

    private inner class VanillaExtraction(context: EntityRendererProvider.Context) :
        LivingEntityRenderer<T, PosableLivingRenderState, ExtractionModel>(context, ExtractionModel(), 0f) {
        var fallAngle = 90f
        override fun createRenderState() = PosableLivingRenderState()
        override fun getTextureLocation(state: PosableLivingRenderState): Identifier = error("Extraction adapter is never submitted")
        override fun getFlipDegrees() = fallAngle
        override fun shouldShowName(entity: T, distanceToCameraSq: Double) = this@PosableLivingEntityRenderer.shouldShowName(entity, distanceToCameraSq)
        fun defaultShowName(entity: T, distanceToCameraSq: Double) = super.shouldShowName(entity, distanceToCameraSq)
        fun applyRotations(state: PosableLivingRenderState, pose: PoseStack) = setupRotations(state, pose, state.bodyRot, state.scale)
    }
}
