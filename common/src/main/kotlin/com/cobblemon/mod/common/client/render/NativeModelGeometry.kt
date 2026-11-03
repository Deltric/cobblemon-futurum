/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.BlockModelResolver
import net.minecraft.client.renderer.block.model.BlockDisplayContext
import net.minecraft.client.renderer.block.ModelBlockRenderer
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.resources.model.geometry.QuadCollection
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/** Native models are resolved during extraction and owned by the resulting scene. */
object NativeModelGeometry {
    private val blockDisplayContext = BlockDisplayContext.create()

    fun extractItem(stack: ItemStack, display: ItemDisplayContext, light: Int, overlay: Int,
                    pose: PoseStack, geometry: ModelGeometry.Builder, level: Level?, seed: Int) {
        val state = ItemStackRenderState()
        Minecraft.getInstance().itemModelResolver.updateForTopItem(state, stack, display, level, null, seed)
        geometry.addItem(state, pose, light, overlay)
    }

    fun extractBlock(state: BlockState, pose: PoseStack, geometry: ModelGeometry.Builder, light: Int, overlay: Int, tint: Int? = null) {
        val model = BlockModelRenderState()
        BlockModelResolver(Minecraft.getInstance().modelManager).update(model, state, blockDisplayContext)
        if (tint != null) {
            val colors = model.tintLayers()
            if (colors.isEmpty()) colors.add(ARGB.opaque(tint))
            else for (i in 0 until colors.size) colors.set(i, ARGB.opaque(tint))
        }
        geometry.addBlock(model, pose, light, overlay)
    }

    fun extractWorldBlock(state: BlockState, level: BlockAndTintGetter, pos: BlockPos, pose: PoseStack, geometry: ModelGeometry.Builder) {
        val minecraft = Minecraft.getInstance()
        val model = minecraft.modelManager.blockStateModelSet.get(state)
        val renderer = ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), false, minecraft.blockColors)
        renderer.tesselateBlock({ x, y, z, quad, instance ->
            pose.pushPose()
            pose.translate(x, y, z)
            geometry.getBuffer(quad.materialInfo().itemRenderType()).putBakedQuad(pose.last(), quad, instance)
            pose.popPose()
        }, 0f, 0f, 0f, level, pos, state, model, state.getSeed(pos))
    }

    fun usesBlockLighting(stack: ItemStack, level: Level?): Boolean {
        val state = ItemStackRenderState()
        Minecraft.getInstance().itemModelResolver.updateForTopItem(state, stack, ItemDisplayContext.GROUND, level, null, 0)
        return state.usesBlockLight()
    }

    fun extractQuads(quads: QuadCollection, pose: PoseStack, geometry: ModelGeometry.Builder,
                     light: Int, overlay: Int, color: Int = -1, material: RenderType? = null) {
        val instance = QuadInstance()
        instance.setLightCoords(light)
        instance.setOverlayCoords(overlay)
        instance.setColor(color)
        for (quad in quads.all) {
            geometry.getBuffer(material ?: quad.materialInfo().itemRenderType()).putBakedQuad(pose.last(), quad, instance)
        }
    }
}
