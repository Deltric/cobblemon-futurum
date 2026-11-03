/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * This class adds children-awareness to a Widget similar like a Screen
 * (otherwise the Widgets do not react on click/hover)
 *
 * Screens select the first child whose [isMouseOver] accepts the pointer before
 * forwarding clicks. Subclasses must include controls outside their own rectangle
 * in that hit test and exclude regions reserved for overlapping sibling controls.
 */
abstract class ParentWidget(
    pX: Int, pY: Int,
    pWidth: Int, pHeight: Int,
    component: Component
): CobblemonRenderable, AbstractWidget(pX, pY, pWidth, pHeight, component) {

    val children: MutableList<GuiEventListener> = mutableListOf()

    /**
     * Adds Widget to the children list
     */
    protected fun addWidget(widget: GuiEventListener) {
        children.add(widget)
    }

    /**
     * Removes Widget from the children list
     */
    protected fun removeWidget(widget: GuiEventListener) {
        children.remove(widget)
    }

    override fun mouseMoved(pMouseX: Double, pMouseY: Double) {
        children.forEach {
            it.mouseMoved(pMouseX, pMouseY)
        }
        super.mouseMoved(pMouseX, pMouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return children.any {
            it.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        } || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        return children.any {
            it.mouseClicked(event, doubleClick)
        }
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return children.any {
            it.mouseReleased(event)
        } || super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, f: Double, g: Double): Boolean {
        return children.any {
            it.mouseDragged(event, f, g)
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        return children.any {
            it.keyPressed(event)
        } || super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        children.forEach {
            it.keyReleased(event)
        }
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        children.forEach {
            it.charTyped(event)
        }
        return super.charTyped(event)
    }

    override fun defaultButtonNarrationText(pNarrationElementOutput: NarrationElementOutput) {
    }

    /**
     * TODO
     *
     * @param mouseX
     * @param mouseY
     * @return
     *
     * @author Licious
     * @since April 29th, 2022
     */
    fun ishHovered(mouseX: Number, mouseY: Number) = mouseX in this.x..(this.x + this.width) && mouseY in this.y..(this.y + this.height)

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}

}
