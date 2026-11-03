/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pc

import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component

class FilterWidget(
    private var pX: Int,
    private var pY: Int,
    text: Component = "FilterWidget".text(),
    update: () -> (Unit)
): TextWidget(pX, pY, text = text, update = update) {

    companion object {
        const val ICON_SIZE = 16
        const val SCALE = 0.5F

        private val iconFilterResource = cobblemonResource("textures/gui/pc/pc_icon_filter.png")
        private val labelFilter = Component.translatable("cobblemon.ui.pc.filter")
    }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        blitk(context = context,
            x = (x - 9) / SCALE,
            y = (y + 3) / SCALE,
            texture = iconFilterResource,
            width = ICON_SIZE,
            height = ICON_SIZE,
            vOffset = if (isFocused || !value.isEmpty() || iconHovered(mouseX, mouseY)) ICON_SIZE else 0,
            textureHeight = ICON_SIZE * 2,
            scale = SCALE)

        val input = applyTextCursor(value, labelFilter)
        val textWidth = Minecraft.getInstance().font.width((if (!isFocused && value.isEmpty()) labelFilter else value.text()).bold().font(CobblemonResources.DEFAULT_LARGE))
        val centerX = x + (width / 2) - (textWidth / 2)
        if (startPosX != centerX) startPosX = centerX

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = input.bold(),
            x = startPosX,
            y = pY + 2
        )

        renderCursor(context, input)

        if (iconHovered(mouseX, mouseY)) {
            context.setTooltipForNextFrame(Minecraft.getInstance().font, lang("ui.pc.filter.tooltip"), mouseX, mouseY)
        }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        isActive && (super.isMouseOver(mouseX, mouseY) || iconHovered(mouseX.toInt(), mouseY.toInt()))

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        if (iconHovered(mouseX.toInt(), mouseY.toInt())) {
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1.0F, 0.25F))
            // Simulate clicking on text area if icon is clicked
            if (!isFocused) return super.mouseClicked(MouseButtonEvent(x + (width / 2.0), y + (height / 2.0), event.buttonInfo()), doubleClick)
            // The icon previously lay outside the EditBox hit rectangle, so a
            // second click ended editing. Preserve that behavior explicitly.
            unfocused()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    fun iconHovered(mouseX: Int, mouseY: Int): Boolean = mouseX >= (x - 9) && mouseY >= (y + 3)
        && mouseX <= ((x - 9) + (ICON_SIZE * SCALE)) && mouseY <= ((y + 3) + (ICON_SIZE * SCALE))
}
