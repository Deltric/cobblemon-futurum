/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.cookingpot

import com.cobblemon.mod.common.api.gui.textColour
import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonNetwork.sendToServer
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.block.campfirepot.CampfirePotColor
import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu
import com.cobblemon.mod.common.block.campfirepot.SeasoningSlot
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.COOKING_POT_COLOR_INDEX
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.IS_LID_OPEN_INDEX
import com.cobblemon.mod.common.integration.jei.CobblemonJeiPlugin
import com.cobblemon.mod.common.integration.jei.cooking.CampfirePotRecipeCategory
import com.cobblemon.mod.common.item.crafting.CookingPotRecipe
import com.cobblemon.mod.common.net.messages.client.cooking.ToggleCookingPotLidPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.WidgetSprites
import com.cobblemon.mod.common.client.gui.CobblemonContainerScreen
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.inventory.RecipeBookMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.crafting.CraftingInput
import kotlin.math.ceil

@Environment(EnvType.CLIENT)
class CookingPotScreen(
    menu: CookingPotMenu,
    playerInventory: Inventory,
    title: Component
) : CobblemonContainerScreen<CookingPotMenu>(
    menu,
    playerInventory,
    Component.translatable("cobblemon.container.campfire_pot"),
    BACKGROUND_WIDTH, BACKGROUND_HEIGHT
), RecipeUpdateListener {

    companion object {
        private const val BACKGROUND_HEIGHT = 166
        private const val BACKGROUND_WIDTH = 176

        const val COOK_PROGRESS_HEIGHT = 12
        const val COOK_PROGRESS_WIDTH = 22

        private val BACKGROUND = cobblemonResource("textures/gui/campfirepot/campfire_pot.png")
        val COOK_PROGRESS_SPRITE: Identifier = cobblemonResource("textures/gui/campfirepot/cook_progress.png")

    }

    private val recipeBookComponent = CookingPotRecipeBookComponent(menu)
    private var widthTooNarrow: Boolean = false

    private lateinit var cookButton: CookButton

    override fun containerTick() {
        super.containerTick()
        this.recipeBookComponent.tick()
    }

    override fun init() {
        super.init()
        this.widthTooNarrow = this.width < 379
        this.recipeBookComponent.init(
            this.width,
            this.height,
            this.minecraft,
            this.widthTooNarrow
        )

        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth)
        val topPos = ((height - BACKGROUND_HEIGHT) / 2)

        val recipeBookButton = ImageButton(
            this.leftPos + 5, topPos + 35, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES
        ) { button ->
            this.recipeBookComponent.toggleVisibility()
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth)
            button.setPosition(this.leftPos + 5, topPos + 35)
        }

        this.addWidget(this.recipeBookComponent)
        this.addRenderableWidget(recipeBookButton)

        this.titleLabelX = this.imageWidth - (BACKGROUND_WIDTH / 2) - (this.font.width(this.title) / 2)
        this.titleLabelY = 6

        if (::cookButton.isInitialized) removeWidget(cookButton)
        val color = CampfirePotColor.entries[menu.containerData.get(COOKING_POT_COLOR_INDEX).coerceIn(0, CampfirePotColor.entries.lastIndex)]
        cookButton = CookButton(this.leftPos + 97, topPos + 56, menu.containerData.get(IS_LID_OPEN_INDEX) == 0, color) {
            val isLidClosed = menu.containerData.get(IS_LID_OPEN_INDEX) == 0
            sendToServer(ToggleCookingPotLidPacket(isLidClosed))
        }

        addRenderableWidget(cookButton)
    }

    override fun extractLabels(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, textColour(4210752), false)
    }

    private fun extractCookingBackground(
        context: GuiGraphicsExtractor,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int
    ) {
        blitk(context = context,
            texture = BACKGROUND,
            x = leftPos,
            y = (height - BACKGROUND_HEIGHT) / 2,
            width = BACKGROUND_WIDTH,
            height = BACKGROUND_HEIGHT)

        val cookProgress = ceil(menu.getBurnProgress() * COOK_PROGRESS_WIDTH).toInt()
        blitk(context = context,
            texture = COOK_PROGRESS_SPRITE,
            x = leftPos + 96,
            y = topPos + 39,
            width = cookProgress,
            height = COOK_PROGRESS_HEIGHT,
            textureWidth = COOK_PROGRESS_WIDTH)
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {
        cookButton.color = CampfirePotColor.entries[menu.containerData.get(COOKING_POT_COLOR_INDEX).coerceIn(0, CampfirePotColor.entries.lastIndex)]
        cookButton.selected = menu.containerData.get(IS_LID_OPEN_INDEX) == 0
        cookButton.setPosition(this.leftPos + 97, topPos + 56)

        if (this.recipeBookComponent.isVisible && this.widthTooNarrow) {
            this.recipeBookComponent.extractRenderState(context, mouseX, mouseY, partialTicks)
        } else {
            extractCookingBackground(context, partialTicks, mouseX, mouseY)
            super.extractContents(context, mouseX, mouseY, partialTicks)
            this.recipeBookComponent.extractRenderState(context, mouseX, mouseY, partialTicks)
            context.pose().pushMatrix()
            context.pose().translate(leftPos.toFloat(), topPos.toFloat())
            this.recipeBookComponent.extractGhostRecipe(context, true)
            context.pose().popMatrix()
        }

        val resultSlot = menu.slots[menu.resultSlotIndex]
        if (!menu.previewItem.isEmpty && !resultSlot.hasItem() && !(recipeBookComponent.isVisible && widthTooNarrow)) {
            val resultItem = menu.previewItem

            // Use the native recipe-book ghost treatment for an unavailable result.
            val itemX = leftPos + resultSlot.x
            val itemY = topPos + resultSlot.y
            context.fill(itemX, itemY, itemX + 16, itemY + 16, 822018048)
            context.fakeItem(resultItem, itemX, itemY)
            context.fill(itemX, itemY, itemX + 16, itemY + 16, 822083583)
            context.itemDecorations(font, resultItem, itemX, itemY)

            if (isHovering(resultSlot.x, resultSlot.y, 16, 16, mouseX.toDouble(), mouseY.toDouble()) && menu.carried.isEmpty) {
                context.setTooltipForNextFrame(font, getTooltipFromContainerItem(resultItem), resultItem.tooltipImage, mouseX, mouseY)
            }
        }

        if (!(recipeBookComponent.isVisible && widthTooNarrow)) extractCarriedItem(context, mouseX, mouseY)
        this.extractTooltip(context, mouseX, mouseY)
        this.recipeBookComponent.extractTooltip(context, mouseX, mouseY, hoveredSlot)
    }

    override fun extractSlot(guiGraphics: GuiGraphicsExtractor, slot: Slot, mouseX: Int, mouseY: Int) {
        if (slot is SeasoningSlot && ((!menu.carried.isEmpty && !slot.mayPlace(menu.carried)) || (slot.hasItem() && !slot.mayPlace(slot.item))) ) {
            val x = slot.x
            val y = slot.y
            guiGraphics.fill(x,y,x+16,y+16,822018048)
        }
        super.extractSlot(guiGraphics, slot, mouseX, mouseY)
    }

    override fun recipesUpdated() {
        this.recipeBookComponent.recipesUpdated()
    }

    override fun fillGhostRecipe(display: RecipeDisplay) = recipeBookComponent.fillGhostRecipe(display)

    fun getRecipeBookComponent(): RecipeBookComponent<CookingPotMenu> = this.recipeBookComponent

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val progressX = leftPos + 96
        val progressY = topPos + 39
        if (Cobblemon.implementation.isModInstalled("jei") &&
            mouseX >= progressX && mouseX < progressX + COOK_PROGRESS_WIDTH &&
            mouseY >= progressY && mouseY < progressY + COOK_PROGRESS_HEIGHT
        ) {
            CobblemonJeiPlugin.jeiRuntime?.recipesGui?.showTypes(
                listOf(CampfirePotRecipeCategory.RECIPE_TYPE)
            )
            return true
        }

        if (this.recipeBookComponent.mouseClicked(event, doubleClick)) {
            this.focused = this.recipeBookComponent

            return true
        } else {
            return if (this.widthTooNarrow && this.recipeBookComponent.isVisible) true else super.mouseClicked(event, doubleClick)
        }
    }

    override fun extractTooltip(
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int
    ) {
        val progressX = leftPos + 96
        val progressY = topPos + 39
        if (Cobblemon.implementation.isModInstalled("jei") &&
            mouseX >= progressX && mouseX < progressX + COOK_PROGRESS_WIDTH &&
            mouseY >= progressY && mouseY < progressY + COOK_PROGRESS_HEIGHT
        ) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                Component.translatable("jei.tooltip.show.recipes"),
                mouseX,
                mouseY
            )
            return
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY)
    }

    // Outside clicks carry a null slot; the Java base declares vanilla's actual contract.
    override fun slotClicked(slot: Slot?, slotId: Int, mouseButton: Int, type: ContainerInput) {
        super.slotClicked(slot, slotId, mouseButton, type)
        recipeBookComponent.slotClicked(slot)
    }
}
