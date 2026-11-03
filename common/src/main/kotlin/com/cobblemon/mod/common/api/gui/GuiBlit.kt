/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.api.gui

import com.cobblemon.mod.common.mixin.accessor.GuiGraphicsExtractorAccessor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

/** Records a textured GUI quad without rounding fractional dimensions or texture coordinates. */
@JvmOverloads
fun blitk(
    context: GuiGraphicsExtractor,
    texture: Identifier,
    x: Number,
    y: Number,
    height: Number = 0,
    width: Number = 0,
    uOffset: Number = 0,
    vOffset: Number = 0,
    textureWidth: Number = width,
    textureHeight: Number = height,
    red: Number = 1,
    green: Number = 1,
    blue: Number = 1,
    alpha: Number = 1F,
    blend: Boolean = true,
    scale: Float = 1F
) {
    if (width.toFloat() == 0F || height.toFloat() == 0F || alpha.toFloat() <= 0F) return
    val matrices = context.pose()
    matrices.pushMatrix()
    try {
        matrices.scale(scale, scale)
        matrices.translate(x.toFloat(), y.toFloat())
        matrices.scale(width.toFloat(), height.toFloat())
        (context as GuiGraphicsExtractorAccessor).`cobblemon$blit`(
            if (blend) RenderPipelines.GUI_TEXTURED else RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND,
            texture, 0, 1, 0, 1,
            uOffset.toFloat() / textureWidth.toFloat(),
            (uOffset.toFloat() + width.toFloat()) / textureWidth.toFloat(),
            vOffset.toFloat() / textureHeight.toFloat(),
            (vOffset.toFloat() + height.toFloat()) / textureHeight.toFloat(),
            ARGB.colorFromFloat(alpha.toFloat(), red.toFloat(), green.toFloat(), blue.toFloat())
        )
    } finally {
        matrices.popMatrix()
    }
}

/** Existing Cobblemon text colours use RGB unless their alpha is explicitly supplied. */
fun textColour(colour: Int): Int = if (colour ushr 24 == 0) colour or -0x1000000 else colour
