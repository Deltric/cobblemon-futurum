/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import com.cobblemon.mod.common.api.gui.scrollAmount
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.Mth

abstract class ScrollingWidget<T : ObjectSelectionList.Entry<T>>(
    top: Int = 0,
    val left: Int = 0,
    width: Int = 10,
    height: Int = 10,
    slotHeight: Int = 10,
    val scrollBarWidth: Int = 5
) : ObjectSelectionList<T>(Minecraft.getInstance(), width, height, top, slotHeight), CobblemonRenderable {
    override fun extractListBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractSelection(graphics: GuiGraphicsExtractor, entry: T, outlineColor: Int) {}
    override fun extractListSeparators(graphics: GuiGraphicsExtractor) {}

    init {
        setRectangle(width, height, top, top + height)
        setLeft(left)
    }

    final override fun setRectangle(width: Int, height: Int, top: Int, bottom: Int) {
        this.width = width
        this.height = height
        this.y = bottom
        this.x = left + width
    }

    fun setLeft(left: Int) { this.x = left }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        focused = if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) getEntryAtPosition(mouseX.toDouble(), mouseY.toDouble()) else null
        enableScissor(context)
        extractListItems(context, mouseX, mouseY, delta)
        context.disableScissor()
        if (maxScrollAmount() > 0) renderScrollbar(context, mouseX, mouseY, delta)
    }

    open fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val xLeft = scrollBarX()
        val xRight = xLeft + scrollBarWidth
        val barHeight = bottom - y
        var thumbHeight = ((barHeight * barHeight).toFloat() / (contentHeight() - 4).toFloat()).toInt()
        thumbHeight = Mth.clamp(thumbHeight, 32, barHeight - 8)
        val thumbY = (scrollAmount.toInt() * (barHeight - thumbHeight) / maxScrollAmount() + y).coerceAtLeast(y)
        context.fill(xLeft, y, xRight, bottom, -16777216)
        context.fill(xLeft, thumbY, xRight, thumbY + thumbHeight, -8355712)
        context.fill(xLeft, thumbY, xRight - 1, thumbY + thumbHeight - 1, -4144960)
    }

    open fun renderHorizontalShadows(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.blit(RenderPipelines.GUI_TEXTURED, Screen.MENU_BACKGROUND, left, 0, 0F, 0F, width, y, 32, 32, -12566464)
        context.blit(RenderPipelines.GUI_TEXTURED, Screen.MENU_BACKGROUND, left, bottom, 0F, bottom.toFloat(), width, height - bottom, 32, 32, -12566464)
        context.fillGradient(left, y, right, y + 4, -16777216, 0)
        context.fillGradient(left, bottom - 4, right, bottom, 0, -16777216)
    }

    override fun extractListItems(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        for (index in 0 until itemCount) {
            val rowTop = getRowTop(index)
            if (getRowBottom(index) >= y && rowTop <= bottom) {
                extractEntry(context, mouseX, mouseY, delta, index, rowLeft, rowTop, rowWidth, defaultEntryHeight)
            }
        }
    }

    protected open fun extractEntry(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float,
                                    index: Int, x: Int, y: Int, entryWidth: Int, entryHeight: Int) {
        val entry = getEntry(index)
        entry.x = x
        entry.y = y
        entry.setWidth(entryWidth)
        entry.setHeight(entryHeight)
        entry.extractContent(context, mouseX, mouseY, focused == entry, delta)
    }

    open fun getEntry(index: Int): T = children()[index]
    override fun getRowLeft(): Int = left
    override fun getRowRight(): Int = rowLeft + rowWidth
    override fun getRowWidth(): Int = width
    override fun getRowTop(index: Int): Int = y - scrollAmount.toInt() + index * defaultEntryHeight
    override fun getRowBottom(index: Int): Int = getRowTop(index) + defaultEntryHeight
    override fun scrollBarX(): Int = left + width - scrollBarWidth

    abstract class Slot<T : Slot<T>> : Entry<T>() {
        // These compact Cobblemon rows deliberately fill their bounds without vanilla's2px inset.
        override fun getContentX(): Int = x
        override fun getContentY(): Int = y
        override fun getContentWidth(): Int = width
        override fun getContentHeight(): Int = height
    }
}
