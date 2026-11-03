/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.npc.widgets

import com.cobblemon.mod.common.api.gui.scrollAmount
import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable.MoLangVariableType
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.client.gui.npc.NPCEditorButton
import com.cobblemon.mod.common.client.gui.npc.NPCEditorScreen
import com.cobblemon.mod.common.client.gui.npc.widgets.ConfigVariableList.ConfigVariable
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import com.cobblemon.mod.common.api.gui.FilteredEditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.WidgetTooltipHolder
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry

class ConfigVariableList(
    val listX: Int,
    var listY: Int,
    val parent: NPCEditorScreen
) : ContainerObjectSelectionList<ConfigVariable>(
    Minecraft.getInstance(),
    WIDTH, // width
    HEIGHT, // height
    0, // top
    SLOT_HEIGHT + SLOT_SPACING
), CobblemonRenderable {
    companion object {
        const val WIDTH = 211
        const val HEIGHT = 160
        const val SLOT_WIDTH = 196
        const val SLOT_HEIGHT = 33
        const val SLOT_SPACING = 0

        val textInputBackground = cobblemonResource("textures/gui/npc/text_input_base.png")
    }

    private var scrolling = false

    override fun getRowWidth() = SLOT_WIDTH

    init {
        listY += 4
        this.x = listX
        this.y = listY
        correctSize()

        parent.dto.registeredVariables.sortedBy { it.category.string }.forEach { variable ->
            val value = parent.dto.variables[variable.variableName] ?: variable.defaultValue
            addEntry(ConfigVariable(variable, value, this))
        }
    }

    override fun scrollBarX() = x + width - 3

    public override fun addEntry(entry: ConfigVariable) = super.addEntry(entry)
    public override fun removeEntry(entry: ConfigVariable) = super.removeEntry(entry)

    override fun extractListSeparators(guiGraphics: GuiGraphicsExtractor) {}

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        correctSize()

        super.extractWidgetRenderState(context, mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        updateScrollingState(mouseX, mouseY)
        if (scrolling) {
            focused = getEntryAtPosition(mouseX, mouseY)
            isDragging = true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val mouseY = event.y()
        if (scrolling) {
            if (mouseY < this.listY) {
                scrollAmount = 0.0
            } else if (mouseY > bottom) {
                scrollAmount = maxScrollAmount().toDouble()
            } else {
                scrollAmount += deltaY
            }
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    private fun updateScrollingState(mouseX: Double, mouseY: Double) {
        scrolling = mouseX >= this.scrollBarX().toDouble()
                && mouseX < (this.scrollBarX() + 3).toDouble()
                && mouseY >= listY
                && mouseY < bottom
    }

    override fun extractListBackground(context: GuiGraphicsExtractor) {}

    private fun correctSize() {
        setRectangle(WIDTH, HEIGHT, listX, (listY - 4))
    }

    fun isHovered(mouseX: Double, mouseY: Double) = mouseX.toFloat() in (x.toFloat()..(x.toFloat() + WIDTH)) && mouseY.toFloat() in (y.toFloat()..(y.toFloat() + HEIGHT))

    class ConfigVariable(val variable: MoLangConfigVariable, val value: String, private val parent: ConfigVariableList) : Entry<ConfigVariable>() {
        val client: Minecraft = Minecraft.getInstance()
        var _focused = false
        var children = mutableListOf<GuiEventListener>()

        var textValue = value
        var booleanValue = value.let { it.toDoubleOrNull() == 1.0 || it.toBooleanStrictOrNull() == true }

        val tooltip = Tooltip.create(variable.description)
        val tooltipHolder = WidgetTooltipHolder().also { it.set(tooltip) }

        val editBox = FilteredEditBox(
            client.font,
            parent.listX,
            parent.listY + 8,
            SLOT_WIDTH - 8,
            SLOT_HEIGHT - 8,
            variable.displayName,
        ).also {
            it.isBordered = false
            it.height = SLOT_HEIGHT - 16
            it.setMaxLength(250)
            it.value = value
            it.setResponder {
                textValue = it
                parent.parent.dto.variables[variable.variableName] = it
            }
            if (variable.type == MoLangVariableType.NUMBER) {
                it.setFilter { value -> value.toDoubleOrNull() != null || value.isBlank() || value == "." || value == "-" }
            }
        }

        val toggleButton = NPCEditorButton(
            parent.listX.toFloat(),
            parent.listY + 6F,
            variable.displayName.copy(),
            booleanValue,
            SLOT_WIDTH
        ) {
            booleanValue = !booleanValue
            parent.parent.dto.variables[variable.variableName] = if (booleanValue) "1" else "0"
            (it as NPCEditorButton).cycleButtonState = booleanValue
        }.also {
            it.setTooltip(tooltip)
        }

        init {
            if (variable.type == MoLangVariableType.BOOLEAN) {
                children.add(toggleButton) // children.add(cycleButton)
            } else {
                children.add(editBox)
            }
        }

        override fun isFocused() = _focused
        override fun setFocused(focused: Boolean) {
            this._focused = focused
        }

        override fun children() = children
        override fun narratables(): List<NarratableEntry> {
            return children.filterIsInstance<NarratableEntry>()
        }

        override fun extractContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, isHovered: Boolean, partialTicks: Float) {
            val rowTop = this.contentY
            val rowLeft = this.contentX
            val x = rowLeft - 4
            val y = rowTop
            if (variable.type == MoLangVariableType.BOOLEAN) {
                toggleButton.x = x
                toggleButton.y = y + 9
                toggleButton.extractRenderState(context, mouseX, mouseY, partialTicks)
            } else {
                drawScaledText(
                    context = context,
                    text = variable.displayName.visualOrderText,
                    x = x,
                    y = y + 4,
                    shadow = true
                )

                blitk(context = context,
            texture = textInputBackground,
            x = x,
            y = y + 16,
            height = 12,
            width = 1)
                blitk(context = context,
            texture = textInputBackground,
            x = x + 1,
            y = y + 15,
            height = 14,
            width = SLOT_WIDTH - 2)
                blitk(context = context,
            texture = textInputBackground,
            x = x + SLOT_WIDTH - 1,
            y = y + 16,
            height = 12,
            width = 1)

                editBox.x = x + 4
                editBox.y = y + 18
                editBox.extractRenderState(context, mouseX, mouseY, partialTicks)

                // Manually renders the tooltip for the edit box because disabling its border messes everything up
                val isEditBoxHovered = mouseX >= x && mouseX <= x + SLOT_WIDTH - 1 && mouseY >= y + 15 && mouseY <= y + 15 + 14
                tooltipHolder.refreshTooltipForNextRenderPass(context, mouseX, mouseY, isEditBoxHovered, editBox.isFocused, editBox.rectangle)
            }
        }
    }
}