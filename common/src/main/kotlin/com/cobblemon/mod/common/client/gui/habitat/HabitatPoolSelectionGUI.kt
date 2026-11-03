/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.habitat

import com.cobblemon.mod.common.api.gui.textColour
import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.api.habitats.HabitatPool
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.lang
import java.awt.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class HabitatPoolSelectionGUI(
    private val isActivated: Boolean,
    private val pools: Map<Identifier, HabitatPool>,
    private val selectedPoolId: Identifier?,
    private val parentScreen: Screen,
    private val onSelect: (Identifier?) -> Unit
) : Screen(lang("ui.edit.habitat.pool.select")), CobblemonRenderable {
    private val headerHeight = 24
    private val footerHeight = 44
    private val listPadding = 6
    private val backButtonHeight = 20
    private val backButtonBottomMargin = 10
    private lateinit var poolList: PoolList

    override fun init() {
        super.init()
        val listTop = headerHeight + listPadding
        val listBottom = getFooterTop() - listPadding
        val listHeight = (listBottom - listTop).coerceAtLeast(24)
        val listWidth = width - 20
        val options = getOptions()

        poolList = addRenderableWidget(
            PoolList(
                left = 10,
                top = listTop,
                listWidth = listWidth,
                listHeight = listHeight,
                options = options,
                selectedPoolId = selectedPoolId,
                onSelect = { poolId ->
                    onSelect(poolId)
                    Minecraft.getInstance().gui.setScreen(parentScreen)
                }
            )
        )

        addRenderableWidget(
            Button.builder(lang("ui.edit.habitat.pool.back")) {
                Minecraft.getInstance().gui.setScreen(parentScreen)
            }.bounds(10, height - (backButtonHeight + backButtonBottomMargin), listWidth, backButtonHeight).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(0, 0, width, height, Color(0, 0, 0, 100).rgb)
        guiGraphics.fill(0, 0, width, headerHeight, Color(0, 0, 0, 150).rgb)
        guiGraphics.fill(0, getFooterTop(), width, height, Color(0, 0, 0, 150).rgb)
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.centeredText(font, title, width / 2, 8, textColour(0xFFFFFF))
    }

    private fun getFooterTop(): Int {
        return height - footerHeight
    }

    private fun getOptions(): List<Pair<Identifier?, String>> {
        val options = mutableListOf<Pair<Identifier?, String>>()
        options.add(null to I18n.get("cobblemon.ui.edit.habitat.pool.custom"))
        val pools = pools.entries.map { it.key to I18n.get(it.value.name) }.sortedBy { it.second.lowercase() }
        options.addAll(pools)
        return options
    }

    private class PoolList(
        private val left: Int,
        private val top: Int,
        private val listWidth: Int,
        private val listHeight: Int,
        options: List<Pair<Identifier?, String>>,
        selectedPoolId: Identifier?,
        private val onSelect: (Identifier?) -> Unit
    ) : ObjectSelectionList<PoolList.PoolEntry>(
        Minecraft.getInstance(),
        listWidth,
        listHeight,
        0,
        20
    ), CobblemonRenderable {
        init {
            x = left
            y = top
            setRectangle(listWidth, listHeight, left, top)
            options.forEach { (poolId, poolName) ->
                addEntry(PoolEntry(poolId, poolName, onSelect))
            }
            setSelected(children().firstOrNull { it.poolId == selectedPoolId })
        }

        override fun extractListBackground(guiGraphics: GuiGraphicsExtractor) {}

        override fun getRowWidth(): Int {
            return listWidth - 8
        }

        override fun scrollBarX(): Int {
            return left + listWidth - 6
        }

        private class PoolEntry(
            val poolId: Identifier?,
            private val poolName: String,
            private val onSelect: (Identifier?) -> Unit
        ) : ObjectSelectionList.Entry<PoolEntry>() {
            override fun extractContent(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
                val rowTop = this.contentY
                val rowLeft = this.contentX
                guiGraphics.text(Minecraft.getInstance().font, poolName.asTranslated(), rowLeft + 4, rowTop + 6, textColour(0xFFFFFF))
            }

            override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
                val mouseX = event.x()
                val mouseY = event.y()
                if (!isMouseOver(mouseX, mouseY)) {
                    return false
                }
                onSelect(poolId)
                return true
            }

            override fun getNarration(): Component {
                return poolName.text()
            }
        }
    }
}
