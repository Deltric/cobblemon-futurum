/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.api.mulch.MulchVariant
import com.cobblemon.mod.common.block.BerryBlock
import com.cobblemon.mod.common.block.entity.BerryBlockEntity
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases.BERRY_SPRITE_ATLAS
import com.cobblemon.mod.common.client.render.models.blockbench.repository.BerryModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.setPosition
import com.cobblemon.mod.common.util.math.geometry.Axis
import com.cobblemon.mod.common.util.toVec3d
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.NativeModelGeometry
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


class BerryBlockRenderer(private val context: BlockEntityRendererProvider.Context) : GeometryBlockEntityRenderer<BerryBlockEntity>() {

    val mulchModels = mutableMapOf(
        MulchVariant.COARSE to CobblemonBakingOverrides.COARSE_MULCH,
        MulchVariant.GROWTH to CobblemonBakingOverrides.GROWTH_MULCH,
        MulchVariant.HUMID to CobblemonBakingOverrides.HUMID_MULCH,
        MulchVariant.LOAMY to CobblemonBakingOverrides.LOAMY_MULCH,
        MulchVariant.PEAT to CobblemonBakingOverrides.PEAT_MULCH,
        MulchVariant.RICH to CobblemonBakingOverrides.RICH_MULCH,
        MulchVariant.SANDY to CobblemonBakingOverrides.SANDY_MULCH,
        MulchVariant.SURPRISE to CobblemonBakingOverrides.SURPRISE_MULCH,
        MulchVariant.NONE to null
    )


    override fun shouldRender(blockEntity: BerryBlockEntity, pos: Vec3): Boolean {
        return super.shouldRender(blockEntity, pos)
                && Minecraft.getInstance().gameRenderer.mainCamera().cullFrustum.isVisible(AABB.ofSize(blockEntity.blockPos.toVec3d(), 2.0, 4.0, 2.0))
    }

    override fun extractGeometry(entity: BerryBlockEntity, tickDelta: Float, matrices: PoseStack, vertexConsumers: ModelGeometry.Builder, light: Int, overlay: Int) {
        if (!shouldRender(entity, entity.blockPos.toVec3d())) return
        val blockState = entity.blockState
        // RenderTypes.entityCutoutNoCull for shading, as shading is absent in RenderType CobblemonRenderLayers.BERRY_LAYER
        val buffer = vertexConsumers.getBuffer(
            RenderTypes.entityCutout(BERRY_SPRITE_ATLAS.textureAtlas.location())
        )

        renderToBuffer(entity, matrices, light, overlay, buffer)
        drawMulch(matrices, vertexConsumers, entity, light, overlay)
    }

    private fun drawMulch(
        matrices: PoseStack,
        vertexConsumers: ModelGeometry.Builder,
        entity: BerryBlockEntity,
        light: Int,
        overlay: Int
    ) {
        matrices.pushPose()
        //Mulch is rendered on a different layer than the actual berries so
        mulchModels[entity.mulchVariant]?.let {
            NativeModelGeometry.extractQuads(it.getModel(), matrices, vertexConsumers, light, overlay)
        }
        matrices.popPose()
    }


    fun renderToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer) {
        if (entity.blockState.getValue(BerryBlock.AGE) == 0) {
            renderBabyToBuffer(entity, matrices, light, overlay, buffer)
        }
        else if (entity.blockState.getValue(BerryBlock.AGE) > BerryBlock.MATURE_AGE){
            renderAdultToBuffer(entity, matrices, light, overlay, buffer)
        }
    }

    fun renderBabyToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer) {
//        val bufferBuilder = Tesselator.getInstance().begin(CobblemonRenderLayers.BERRY_LAYER.mode(), CobblemonRenderLayers.BERRY_LAYER.format())
        val berry = entity.berry() ?: return
        val model = BerryModelRepository.modelOf(berry.fruitModelIdentifier) ?: return
        val pos = berry.stageOnePositioning.position
        model.setPosition(Axis.X_AXIS.ordinal, pos.x.toFloat())
        model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
        model.setPosition(Axis.Z_AXIS.ordinal, pos.z.toFloat())
        val rot = berry.stageOnePositioning.rotation
        model.setRotation(
            Math.toRadians(180 - rot.x).toFloat(),
            Math.toRadians(180 + rot.y).toFloat(),
            Math.toRadians(rot.z).toFloat()
        )
        model.render(matrices, buffer, light, overlay)
//        val bufferBuilderFinal = bufferBuilder.buildOrThrow()
//        buffer.bind()
//        buffer.upload(bufferBuilderFinal)
//        VertexBuffer.unbind()
    }

    fun renderAdultToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer) {
        val blockState = entity.blockState
        val age = blockState.getValue(BerryBlock.AGE)
        if (age <= BerryBlock.MATURE_AGE) {
            return
        }
        val isFlower = age == BerryBlock.FLOWER_AGE
//        val bufferBuilder = Tesselator.getInstance().begin(CobblemonRenderLayers.BERRY_LAYER.mode(), CobblemonRenderLayers.BERRY_LAYER.format())
        for ((berry, growthPoint) in entity.berryAndGrowthPoint()) {
            val model = (if (isFlower) BerryModelRepository.modelOf(berry.flowerModelIdentifier) else BerryModelRepository.modelOf(berry.fruitModelIdentifier)) ?: continue
//            matrices.translate(growthPoint.position.x.toFloat() / 16F, growthPoint.position.y.toFloat() / 16F, growthPoint.position.z.toFloat() / 16F)
//            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(growthPoint.rotation.x.toFloat()))
//            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(growthPoint.rotation.y.toFloat()))
//            matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(growthPoint.rotation.z.toFloat()))

            model.setRotation(
                Math.toRadians(180.0 - growthPoint.rotation.x).toFloat(),
                Math.toRadians(180.0 + growthPoint.rotation.y).toFloat(),
                Math.toRadians(growthPoint.rotation.z).toFloat()
            )
            model.setPosition(Axis.X_AXIS.ordinal, growthPoint.position.x.toFloat())
            model.setPosition(Axis.Y_AXIS.ordinal, growthPoint.position.y.toFloat())
            model.setPosition(Axis.Z_AXIS.ordinal, growthPoint.position.z.toFloat())
            model.render(matrices, buffer, light, overlay)
        }
//        val bufferBuilderFinal = bufferBuilder.buildOrThrow()
//        buffer.bind()
//        buffer.upload(bufferBuilderFinal)
//        VertexBuffer.unbind()
    }

}
