/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex

import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.resources.Identifier

class ScaledButton(
    buttonX: Float,
    buttonY: Float,
    val buttonWidth: Number,
    val buttonHeight: Number,
    var resource: Identifier? = null,
    val scale: Float = 0.5F,
    val silent: Boolean = false,
    val clickAction: Button.OnPress
): Button(buttonX.toInt(), buttonY.toInt(), buttonWidth.toInt(), buttonHeight.toInt(), "".text(), clickAction, DEFAULT_NARRATION), CobblemonRenderable {

    var buttonX: Float = buttonX
        set(value) {
            field = value
            this.x = value.toInt()
        }

    var buttonY: Float = buttonY
        set(value) {
            field = value
            this.y = value.toInt()
        }

    var isWidgetActive = false
    var isVisible = true

    override fun mouseDragged(event: MouseButtonEvent, f: Double, g: Double) = false
    override fun defaultButtonNarrationText(builder: NarrationElementOutput) {
    }

    override fun extractContents(context: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        val matrices = context.pose()
        val texture = resource
        if (isVisible && texture != null) {
            blitk(context = context,
            texture = texture,
            x = buttonX / scale,
            y = buttonY / scale,
            width = buttonWidth,
            height = buttonHeight,
            vOffset = if (isButtonHovered(pMouseX, pMouseY) || isWidgetActive) buttonHeight else 0,
            textureHeight = buttonHeight.toFloat() * 2,
            scale = scale)
        }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        // Native parents choose one child before dispatching the click. Use the
        // same current position and scale as rendering, including both visibility flags.
        return visible && active && isVisible && isButtonHovered(mouseX, mouseY)
    }

    override fun playDownSound(soundManager: SoundManager) {
        if (isVisible && active && !this.silent) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.POKEDEX_CLICK_SHORT, 1.0F))
        }
    }

    fun isButtonHovered(mouseX: Number, mouseY: Number): Boolean {
        return mouseX.toFloat() in (buttonX..(buttonX + (buttonWidth.toFloat() * scale)))
                && mouseY.toFloat() in (buttonY..(buttonY + (buttonHeight.toFloat() * scale)))
    }
}
