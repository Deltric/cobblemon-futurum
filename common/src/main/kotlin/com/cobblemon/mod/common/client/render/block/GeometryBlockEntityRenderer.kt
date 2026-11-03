/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.client.render.ModelGeometry
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

class GeometryBlockEntityRenderState : BlockEntityRenderState() {
    var geometry: ModelGeometry? = null
}

/** Runs block-entity reads and animation only while extracting this frame. */
abstract class GeometryBlockEntityRenderer<T : BlockEntity> : BlockEntityRenderer<T, GeometryBlockEntityRenderState> {
    override fun createRenderState() = GeometryBlockEntityRenderState()

    override fun extractRenderState(entity: T, state: GeometryBlockEntityRenderState, partialTicks: Float, camera: Vec3, breakProgress: ModelFeatureRenderer.CrumblingOverlay?) {
        val overlay = breakProgress?.let { ModelFeatureRenderer.CrumblingOverlay(it.progress(), it.cameraPose().copy()) }
        BlockEntityRenderState.extractBase(entity, state, overlay)
        val geometry = ModelGeometry.Builder()
        extractGeometry(entity, partialTicks, PoseStack(), geometry, state.lightCoords, OverlayTexture.NO_OVERLAY)
        state.geometry = geometry.build()
    }

    protected abstract fun extractGeometry(entity: T, partialTicks: Float, pose: PoseStack, geometry: ModelGeometry.Builder, light: Int, overlay: Int)

    override fun submit(state: GeometryBlockEntityRenderState, pose: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        val geometry = state.geometry ?: return
        val breaking = state.breakProgress
        if (breaking == null) geometry.submit(pose, collector)
        else geometry.submitWithCrumbling(pose, collector, breaking)
    }
}
