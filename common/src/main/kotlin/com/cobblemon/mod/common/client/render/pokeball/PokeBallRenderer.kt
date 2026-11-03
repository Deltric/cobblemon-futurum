/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.pokeball

import com.cobblemon.mod.common.client.entity.EmptyPokeBallClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.pokeball.PosablePokeBallModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
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
import net.minecraft.resources.Identifier

class PokeBallRenderer(context: EntityRendererProvider.Context) : EntityRenderer<EmptyPokeBallEntity, PosableEntityRenderState>(context) {
    val model = PosablePokeBallModel()

    fun getTextureLocation(pEntity: EmptyPokeBallEntity): Identifier {
        return VaryingModelRepository.getTexture(pEntity.pokeBall.name, pEntity.delegate as EmptyPokeBallClientDelegate)
    }

    override fun createRenderState() = PosableEntityRenderState()

    override fun submit(state: PosableEntityRenderState, poseStack: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        state.geometry?.submit(poseStack, collector, state.outlineColor)
        super.submit(state, poseStack, collector, camera)
    }

    override fun extractRenderState(entity: EmptyPokeBallEntity, renderState: PosableEntityRenderState, partialTicks: Float) {
        super.extractRenderState(entity, renderState, partialTicks)
        renderState.geometry = null
        val yaw = entity.getYRot(partialTicks)
        val poseStack = PoseStack()
        val buffer = ModelGeometry.Builder()
        val packedLight = renderState.lightCoords

        val state = entity.delegate as EmptyPokeBallClientDelegate
        this.model.context.put(RenderContext.POSABLE_STATE, state)
        this.model.context.put(RenderContext.ASPECTS, entity.aspects)
        state.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.pokeBall.name, state)
        this.model.posableModel = model
        this.model.posableModel.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        this.model.context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)
        poseStack.pushPose()
        poseStack.rotate(Axis.YP.rotationDegrees(yaw))
        poseStack.scale(0.7F, -0.7F, -0.7F)
        val vertexConsumer = buffer.getBuffer(RenderTypes.entityCutoutCull(getTextureLocation(entity)))
        state.updatePartialTicks(partialTicks)
        model.setLayerContext(buffer, state, VaryingModelRepository.getLayers(entity.pokeBall.name, state))
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