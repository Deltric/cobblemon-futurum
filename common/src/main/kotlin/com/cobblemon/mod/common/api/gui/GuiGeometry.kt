/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import com.cobblemon.mod.common.mixin.accessor.GuiGraphicsExtractorAccessor
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.world.phys.Vec2
import org.joml.Matrix3x2f
import kotlin.math.ceil
import kotlin.math.floor

fun drawGuiTriangle(context: GuiGraphicsExtractor, first: Vec2, second: Vec2, third: Vec2, colour: Int) {
    val accessor = context as GuiGraphicsExtractorAccessor
    val pose = Matrix3x2f(context.pose())
    val scissor = accessor.`cobblemon$getScissorStack`().peek()
    val left = floor(minOf(first.x, second.x, third.x)).toInt()
    val top = floor(minOf(first.y, second.y, third.y)).toInt()
    val right = ceil(maxOf(first.x, second.x, third.x)).toInt()
    val bottom = ceil(maxOf(first.y, second.y, third.y)).toInt()
    val bounds = ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose)
    accessor.`cobblemon$getGuiRenderState`().addGuiElement(object : GuiElementRenderState {
        override fun pipeline() = RenderPipelines.GUI
        override fun textureSetup() = TextureSetup.noTexture()
        override fun scissorArea() = scissor
        override fun bounds() = if (scissor == null) bounds else scissor.intersection(bounds)
        override fun buildVertices(consumer: VertexConsumer) {
            consumer.addVertexWith2DPose(pose, first.x, first.y).setColor(colour)
            consumer.addVertexWith2DPose(pose, second.x, second.y).setColor(colour)
            consumer.addVertexWith2DPose(pose, third.x, third.y).setColor(colour)
            // GUI batches use quads; the repeated vertex makes the second triangle degenerate.
            consumer.addVertexWith2DPose(pose, third.x, third.y).setColor(colour)
        }
    })
}
