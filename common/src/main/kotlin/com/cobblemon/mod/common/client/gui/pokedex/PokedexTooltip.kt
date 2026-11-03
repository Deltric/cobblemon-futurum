/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.ScopedGuiBlur
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import java.util.Optional

private val tooltipEdge = cobblemonResource("textures/gui/pokedex/tooltip_edge.png")
private val tooltipBackground = cobblemonResource("textures/gui/pokedex/tooltip_background.png")

private const val TOOLTIP_SLICE_SIZE = 11
private const val TOOLTIP_MAX_TEXT_WIDTH = 160

fun renderTooltip(
    context: GuiGraphicsExtractor,
    text: MutableComponent,
    posX: Int,
    posY: Int,
    delta: Float,
    offsetY: Int = 0,
) {
    renderTooltip(
        context,
        listOf(text),
        posX,
        posY,
        delta,
        offsetY,
    )
}

fun renderTooltip(
    context: GuiGraphicsExtractor,
    text: List<MutableComponent>,
    posX: Int,
    posY: Int,
    delta: Float,
    offsetY: Int = 0,
    maxTextWidth: Int = TOOLTIP_MAX_TEXT_WIDTH,
) {
    val text = text.flatMap { wrapTooltipLine(it, maxTextWidth) }
    // Only a tooltip that fits on a single line is centered; wrapped text is left-aligned
    val centered = text.size == 1

    val poseStack = context.pose()
    val textWidth = text.maxOf { Minecraft.getInstance().font.width(it.font(CobblemonResources.DEFAULT_LARGE)) }
    val tooltipPadding = 6
    val tooltipWidth = textWidth + tooltipPadding
    val tooltipTop = posY + offsetY
    val totalHeight = TOOLTIP_SLICE_SIZE * text.size

    poseStack.pushMatrix()
    context.nextStratum()
    ScopedGuiBlur.extract(context,
        posX - tooltipWidth / 2, tooltipTop + 1,
        posX - tooltipWidth / 2 + tooltipWidth, tooltipTop + totalHeight - 1)

    blitSlicedVertical(context, tooltipEdge, posX - (tooltipWidth / 2) - 1, tooltipTop, 1, totalHeight)
    blitSlicedVertical(context, tooltipBackground, posX - (tooltipWidth / 2), tooltipTop, tooltipWidth, totalHeight)
    blitSlicedVertical(context, tooltipEdge, posX + (tooltipWidth / 2), tooltipTop, 1, totalHeight)

    text.forEachIndexed { index, it ->
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = it,
            x = if (centered) posX else posX - tooltipWidth / 2 + tooltipPadding / 2,
            y = tooltipTop + 1 + TOOLTIP_SLICE_SIZE * index,
            shadow = true,
            centered = centered
        )
    }

    poseStack.popMatrix()
}

private fun wrapTooltipLine(line: MutableComponent, maxWidth: Int): List<MutableComponent> {
    val styled = line.copy().font(CobblemonResources.DEFAULT_LARGE)
    if (Minecraft.getInstance().font.width(styled) <= maxWidth) return listOf(line)

    return Minecraft.getInstance().font.splitter.splitLines(styled, maxWidth, Style.EMPTY).map { wrapped ->
        val component = Component.empty()
        wrapped.visit({ style, string ->
            component.append(Component.literal(string).withStyle(style))
            Optional.empty<Unit>()
        }, Style.EMPTY)
        component
    }
}

// Stretching the whole 11px image over an arbitrary height thickens the caps as more lines are added, so
// instead the caps are drawn 1:1 and only the flat fill row is stretched to cover the remaining height
private fun blitSlicedVertical(
    context: GuiGraphicsExtractor,
    texture: Identifier,
    x: Number,
    y: Int,
    width: Number,
    totalHeight: Int,
) {
    val middleHeight = totalHeight - 2

    blitk(context = context, texture = texture, x = x, y = y, width = width, height = 1, vOffset = 0, textureHeight = TOOLTIP_SLICE_SIZE)
    blitk(context = context, texture = texture, x = x, y = y + 1, width = width, height = middleHeight, vOffset = middleHeight, textureHeight = TOOLTIP_SLICE_SIZE * middleHeight)
    blitk(context = context, texture = texture, x = x, y = y + 1 + middleHeight, width = width, height = 1, vOffset = TOOLTIP_SLICE_SIZE - 1, textureHeight = TOOLTIP_SLICE_SIZE)
}
