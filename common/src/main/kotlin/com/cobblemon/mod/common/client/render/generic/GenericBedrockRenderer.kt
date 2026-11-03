/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.generic

import com.cobblemon.mod.common.client.entity.GenericBedrockClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.generic.PosableGenericEntityModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.generic.GenericBedrockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture

class GenericBedrockRenderer(context: EntityRendererProvider.Context) : EntityRenderer<GenericBedrockEntity, PosableEntityRenderState>(context) {
    val model = PosableGenericEntityModel()
    fun getTextureLocation(entity: GenericBedrockEntity) = VaryingModelRepository.getTexture(entity.category, (entity.delegate as GenericBedrockClientDelegate))
    override fun createRenderState() = PosableEntityRenderState()

    override fun submit(state: PosableEntityRenderState, poseStack: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        if (state.isInvisible) return
        state.geometry?.submit(poseStack, collector, state.outlineColor)
        super.submit(state, poseStack, collector, camera)
    }

    override fun extractRenderState(entity: GenericBedrockEntity, renderState: PosableEntityRenderState, partialTicks: Float) {
        super.extractRenderState(entity, renderState, partialTicks)
        renderState.geometry = null
        val yaw = entity.getYRot(partialTicks)
        val poseStack = PoseStack()
        val buffer = ModelGeometry.Builder()
        val packedLight = renderState.lightCoords

        if (entity.isInvisible) {
            return
        }

        val state = entity.delegate as GenericBedrockClientDelegate
        state.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.category, state)
        this.model.posableModel = model
        model.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        poseStack.pushPose()
        poseStack.scale(1.0F, -1.0F, 1.0F)
        poseStack.scale(entity.scale, entity.scale, entity.scale)
        poseStack.rotate(Axis.YP.rotationDegrees(yaw))
        val vertexConsumer = buffer.getBuffer(RenderTypes.entityCutoutCull(getTextureLocation(entity)))

        state.updatePartialTicks(partialTicks)
        model.setLayerContext(buffer, state, VaryingModelRepository.getLayers(entity.category, state))
        this.model.setupAnim(entity, 0f, 0f, entity.tickCount + partialTicks, 0F, 0F)
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)

        model.green = 1F
        model.blue = 1F
        model.red = 1F

        model.resetLayerContext()

        poseStack.popPose()
        renderState.geometry = buffer.build()
    }
}