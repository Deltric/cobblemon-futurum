/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.boat

import com.cobblemon.mod.common.entity.boat.CobblemonBoatEntity
import com.cobblemon.mod.common.entity.boat.CobblemonBoatType
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.model.`object`.boat.BoatModel
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.state.BoatRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.Unit
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import org.joml.Quaternionf

class CobblemonBoatRenderState : BoatRenderState() { lateinit var boatType: CobblemonBoatType }

class CobblemonBoatRenderer(ctx: EntityRendererProvider.Context, private val hasChest: Boolean) : EntityRenderer<CobblemonBoatEntity, CobblemonBoatRenderState>(ctx) {

    private val waterPatch = Model.Simple(ctx.bakeLayer(ModelLayers.BOAT_WATER_PATCH), { RenderTypes.waterMask() })
    private val boatModels = hashMapOf<CobblemonBoatType, Pair<Identifier, BoatModel>>()

    init {
        this.shadowRadius = 0.8F
        CobblemonBoatType.values().forEach { type ->
            this.boatModels[type] = generateTextureIdentifier(type, this.hasChest) to generateBoatModel(ctx, type, this.hasChest)
        }
    }

    override fun createRenderState() = CobblemonBoatRenderState()

    override fun extractRenderState(entity: CobblemonBoatEntity, state: CobblemonBoatRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.boatType = entity.boatType
        state.yRot = entity.getYRot(partialTicks)
        state.hurtTime = entity.hurtTime - partialTicks
        state.hurtDir = entity.hurtDir
        state.damageTime = (entity.damage - partialTicks).coerceAtLeast(0f)
        state.bubbleAngle = entity.getBubbleAngle(partialTicks)
        state.isUnderWater = entity.isUnderWater
        state.rowingTimeLeft = entity.getRowingTime(0, partialTicks)
        state.rowingTimeRight = entity.getRowingTime(1, partialTicks)
    }

    override fun submit(state: CobblemonBoatRenderState, matrices: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        matrices.pushPose()
        matrices.translate(0F, 0.375F, 0F)
        matrices.rotate(Axis.YP.rotationDegrees(180F - state.yRot))
        val h = state.hurtTime
        val j = state.damageTime
        if (h > 0F) {
            matrices.rotate(Axis.XP.rotationDegrees(kotlin.math.sin(h) * h * j / 10F * state.hurtDir))
        }
        val k = state.bubbleAngle
        if (!Mth.equal(k, 0F)) {
            matrices.rotate(Quaternionf().setAngleAxis(state.bubbleAngle * 0.017453292F, 1F, 0F, 1F))
        }
        val (identifier, entityModel) = this.boatModels[state.boatType]!!
        matrices.scale(-1F, -1F, 1F)
        matrices.rotate(Axis.YP.rotationDegrees(90F))
        collector.submitModel(entityModel, state, matrices, identifier, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor)
        if (!state.isUnderWater) {
            collector.submitModel(waterPatch, Unit.INSTANCE, matrices, identifier, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor)
        }
        matrices.popPose()
        super.submit(state, matrices, collector, camera)
    }

    companion object {

        private fun generateTextureIdentifier(type: CobblemonBoatType, hasChest: Boolean): Identifier {
            val boatSubPath = if (hasChest) "chest_boat" else "boat"
            val path = "textures/entity/$boatSubPath/${type.name.lowercase()}.png"
            return cobblemonResource(path)
        }

        private fun generateBoatModel(ctx: EntityRendererProvider.Context, type: CobblemonBoatType, hasChest: Boolean): BoatModel {
            val modelLayer = this.createBoatModelLayer(type, hasChest)
            val modelPart = ctx.bakeLayer(modelLayer)
            return BoatModel(modelPart)
        }

        internal fun createBoatModelLayer(type: CobblemonBoatType, hasChest: Boolean): ModelLayerLocation {
            val boatSubPath = if (hasChest) "chest_boat" else "boat"
            val path = "$boatSubPath/${type.name.lowercase()}"
            return ModelLayerLocation(cobblemonResource(path), "main")
        }

    }

}