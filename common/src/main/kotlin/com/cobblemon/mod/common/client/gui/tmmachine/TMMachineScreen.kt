/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tmmachine

import com.cobblemon.mod.common.api.gui.scrollAmount
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.ColourLibrary
import com.cobblemon.mod.common.api.gui.MultiLineLabelK
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.reactive.SettableObservable
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.entity.TMMachineBlockEntity
import com.cobblemon.mod.common.block.tmmachine.TMMachineMenu
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.interact.moveselect.MoveSlotButton
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MovesWidget
import com.cobblemon.mod.common.client.settings.ServerSettings
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.net.messages.client.ui.SetActiveTMPacket
import com.cobblemon.mod.common.net.messages.client.ui.SetTMMachineContainerDataPacket
import com.cobblemon.mod.common.net.messages.server.block.TMMachineTeachMovePacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.isInventoryKeyPressed
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.toRGB
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import com.cobblemon.mod.common.client.gui.CobblemonContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.ceil

class TMMachineScreen(containerMenu: TMMachineMenu, val inventory: Inventory, title: Component) : CobblemonContainerScreen<TMMachineMenu>(containerMenu, inventory, title, WIDTH, HEIGHT) {
    companion object {
        const val HEIGHT = 226
        const val WIDTH = 191
        const val HALF_SCALE = 0.5F

        const val DISC_DIAMETER = 56

        const val LOGO_WIDTH = 86
        const val LOGO_HEIGHT = 64

        const val TYPE_SELECT_MODE = 1
        const val MOVE_SELECT_MODE = 2
        const val TM_BURN_MODE = 3

        const val CRAFT_TICKS = 10 // Length for craft completion animation
        const val RESET_DISC_TICKS = 4 // Length for disc reset animation

        const val SCREEN_SAVER_TIMEOUT_TICKS = 1200 // 60 Seconds
        const val SCREEN_SAVER_WIDTH = 118
        const val SCREEN_SAVER_HEIGHT = 110

        const val DESCRIPTION_SCROLLBAR_WIDTH = 2

        val moveOverlayBar = cobblemonResource("textures/gui/tmmachine/summary_move_overlay_bar.png")

        val baseResource = cobblemonResource("textures/gui/tmmachine/base.png")
        val tmTray = cobblemonResource("textures/gui/tmmachine/tm_tray.png")
        val iconBack = cobblemonResource("textures/gui/tmmachine/icon_back.png")
        val tooltipMoveInfo = cobblemonResource("textures/gui/tmmachine/tooltip_move_info.png")
        val emptyDiscSlot = cobblemonResource("textures/item/tms/blank_disc_empty_slot.png")

        val scrollbarSlide = cobblemonResource("textures/gui/tmmachine/scrollbar_slide.png")
        val scrollbarTrack = cobblemonResource("textures/gui/tmmachine/scrollbar_track.png")

        val discBase = cobblemonResource("textures/gui/tmmachine/tm_base.png")
        val discBorder = cobblemonResource("textures/gui/tmmachine/tm_border.png")
        val discOverlay = cobblemonResource("textures/gui/tmmachine/tm_base_overlay.png")
        val discReflective = cobblemonResource("textures/gui/tmmachine/tm_overlay_reflective.png")
        val discReflectiveRotating = cobblemonResource("textures/gui/tmmachine/tm_overlay_reflective_rotating.png")
        val discRotating = cobblemonResource("textures/gui/tmmachine/tm_overlay_rotating.png")

        val logo = cobblemonResource("textures/gui/tmmachine/logo_tm.png")
        val screenSaver = cobblemonResource("textures/gui/tmmachine/screen_saver_background.png")
        val screenOverlay = cobblemonResource("textures/gui/tmmachine/screen_overlay.png")
        val scanLines = cobblemonResource("textures/gui/tmmachine/scan_lines.png")

        val logoResourceMap: Map<Identifier, List<String>> = mapOf(
            cobblemonResource("textures/gui/tmmachine/logo_ct.png") to listOf("fr"),
            cobblemonResource("textures/gui/tmmachine/logo_mt.png") to listOf("es", "it", "pt")
        )

        fun getLogoResource(langCode: String?): Identifier =
            logoResourceMap.entries.find { it.value.contains(langCode?.substringBefore("_")) }?.key ?: logo
    }

    var heldStackMove: MoveTemplate? = null
    var tmList: SettableObservable<MutableList<TechnicalMachine>> = SettableObservable(mutableListOf())
    var output: ItemStack? = null
    var mode: Int = TYPE_SELECT_MODE
    var sortType: ElementalType? = null
    var selectedPokemon: Pokemon? = null

    var partySlotList: MutableSet<TMPartySlotWidget> = mutableSetOf()

    lateinit var backButton: IconButton
    lateinit var startButton: StartButton
    lateinit var selectedMoveButton: MoveSlotButton
    lateinit var moveDescriptionWidget: MoveDescriptionWidget
    lateinit var typesScrollList: TypesScrollingWidget
    lateinit var movesScrollingList: MovesScrollingWidget
    lateinit var moveSearchWidget: MoveSearchWidget

    var discReflectionFrame: Int = 0
    var discRotationFrame: Int = 0

    var ticksElapsed: Int = 0

    var logoResource: Identifier = logo
    var logoTint: Triple<Double, Double, Double> = ElementalTypes.all().random().secondaryColor.toRGB()
    var logoPosX: Int = 0
    var logoPosY: Int = 0
    var logoSlideRight: Boolean = true
    var logoSlideDown: Boolean = true
    var scanPosY: Int = 0
    var scanLineOffsetY: Double = 0.0

    var initScreen: Boolean = false
    var heldMoveItemChanged = false

    var oldBlankTmSlotItem: Item = Items.AIR // AIR == Empty
    var oldIngredientItems: Array<Item> = arrayOf(Items.AIR, Items.AIR, Items.AIR)
    var oldResultTmSlotItem: Item = Items.AIR

    var moveInfoTM: TechnicalMachine? = null

    override fun init() {

        super.init()

        logoResource = getLogoResource(minecraft?.languageManager?.selected)

        logoPosX = leftPos + 1
        logoPosY = topPos + 1
        scanPosY = topPos + 1

        menu.onSelectedTMUpdate = { updateSelectedTM(it) }

        partySlotList.clear()
        CobblemonClient.storage.party.forEachIndexed { index, pokemon ->
            partySlotList.add(TMPartySlotWidget(
                pX = leftPos - 60,
                pY = topPos + 9 + ((TMPartySlotWidget.HEIGHT + 3) * index),
                pokemon = pokemon,
                onPress = {
                    if ((it as TMPartySlotWidget).clickable  && pokemon != null) {
                        // Teach move if holding TM item, else open learnable moves list for Pokémon
                        if (heldStackMove != null) {
                            heldStackMove?.let { move ->
                                if (canLearnTMMove(move, pokemon) == TMPartySlotWidget.CAN_LEARN) {
                                    CobblemonNetwork.sendToServer(TMMachineTeachMovePacket(pokemon.uuid, menu.carried, move))
                                    // Mark to update client next tick
                                    heldMoveItemChanged = true
                                }
                            }
                        } else if (mode != TM_BURN_MODE) {
                            setMoveList(pokemon = pokemon)
                            setScreenFromMode(MOVE_SELECT_MODE)
                            selectedPokemon = pokemon
                            playSound(CobblemonSounds.GUI_CLICK)
                        }
                    }
                }
            ))
        }

        for (slot in partySlotList) {
            if (!children().contains(slot)) addRenderableWidget(slot)
        }

        moveDescriptionWidget = MoveDescriptionWidget(
            leftPos + 125,
            topPos + 42,
            60,
            34
        )
        if (!children().contains(moveDescriptionWidget)) addRenderableWidget(moveDescriptionWidget)

        typesScrollList = TypesScrollingWidget(
            pX = leftPos + 6,
            pY = topPos + 11,
            setType = { type ->
                sortType = type
                setMoveList()
                setScreenFromMode(MOVE_SELECT_MODE)
            }
        )
        if (!children().contains(typesScrollList)) addRenderableWidget(typesScrollList)

        movesScrollingList = MovesScrollingWidget(
            pX = leftPos + 4,
            pY = topPos + 29,
            tmList,
            setTM = { tm, clicked ->
                if (clicked) {
                    if (tm != menu.currentlySelectedTM) SetActiveTMPacket(tm).sendToServer()
                    setScreenFromMode(TM_BURN_MODE)
                } else {
                    updateMoveInformation(tm)
                }
            }
        )
        if (!children().contains(movesScrollingList)) addRenderableWidget(movesScrollingList)

        moveSearchWidget = MoveSearchWidget(pX = leftPos + 16, pY = topPos + 13) {
            if (::movesScrollingList.isInitialized && moveSearchWidget.isFocused) {
                val input = moveSearchWidget.value
                setMoveList(filter = if (input.isBlank()) null else input)
            }
        }
        if (!children().contains(moveSearchWidget)) addRenderableWidget(moveSearchWidget)

        backButton = IconButton(
            leftPos + 106F,
            topPos + 1F,
            20,
            18,
            iconBack
        ) {
            setScreenFromMode(TYPE_SELECT_MODE)
        }
        if (!children().contains(backButton)) addRenderableWidget(backButton)

        selectedMoveButton = MoveSlotButton(
            leftPos + 6,
            topPos + 6,
            move = menu.currentlySelectedTM?.moveName,
            pp = menu.currentlySelectedTM?.moveName?.pp ?: 0,
            ppAsFraction = false
        ) {
            if ((it as MoveSlotButton).enabled) {
                if (::movesScrollingList.isInitialized && movesScrollingList.children().isEmpty()) setMoveList()
                playSound(CobblemonSounds.GUI_CLICK)
                setScreenFromMode(MOVE_SELECT_MODE)
            }
        }
        if (!children().contains(selectedMoveButton)) addRenderableWidget(selectedMoveButton)

        startButton = StartButton(leftPos + 5F, topPos + 35F) {
            if (!(it as StartButton).disabled) {
                menu.currentlySelectedTM?.let {
                    val burnActive = menu.containerData?.get(TMMachineBlockEntity.BURN_ACTIVE_INDEX) ?: 0
                    CobblemonNetwork.sendToServer(SetTMMachineContainerDataPacket(
                        TMMachineBlockEntity.BURN_ACTIVE_INDEX,
                        burnActive xor 1 // Toggle active state, 1 ⇆ 0
                    ))

                    /*// If button is not active, set if process should repeat or not
                    if (burnActive == 0) {
                        CobblemonNetwork.sendToServer(SetTMMachineContainerDataPacket(
                            TMMachineBlockEntity.REPEAT_PROCESS_INDEX,
                            if (Minecraft.getInstance().hasShiftDown()) 1 else 0
                        ))
                    }*/

                    //CobblemonNetwork.sendToServer(SetActiveTMPacket(it))
                    if (burnActive != 1) playSound(CobblemonSounds.TM_MACHINE_START)
                }
            }
        }
        if (!children().contains(startButton)) addRenderableWidget(startButton)

        setScreenFromMode(mode)

        updateSlotItemMemory()
    }

    override fun containerTick() {
        ticksElapsed++
        scanPosY++
        scanLineOffsetY = (scanLineOffsetY + 0.5) % 6
        if (scanPosY + 1 >= (topPos + SCREEN_SAVER_HEIGHT) * 2) scanPosY = 0

        if (mode == TM_BURN_MODE) {
            menu.currentlySelectedTM?.let { tm ->
                val resultStack = menu.inventory!!.getItem(TMMachineMenu.RESULT_SLOT)
                val craftedTmStack = ItemStack(CobblemonItems.TECHNICAL_MACHINE)
                    .also { TMMoveComponent.setTMMove(it, tm.moveName) }

                var validOutput = resultStack.isEmpty || (
                    !resultStack.isEmpty
                    && (resultStack.count < resultStack.maxStackSize)
                    && ItemStack.isSameItemSameComponents(resultStack, craftedTmStack)
                )
                var validCost = !menu.inventory!!.getItem(TMMachineMenu.BLANK_TM_SLOT).isEmpty // Blank TM slot

                if (validOutput && validCost) {
                    val recipes = tm.getClampedRecipe() ?: listOf()
                    recipes.forEachIndexed { index, recipe ->
                        val slotStack = menu.inventory!!.getItem(index + TMMachineMenu.INGREDIENT_SLOTS.first)
                        if (!recipe.ingredient.test(slotStack) || slotStack.count < recipe.count) {
                            validCost = false
                            return@forEachIndexed
                        }
                    }
                }

                if (::startButton.isInitialized) {
                    val discResetting = menu.getPostCraftTicks() > CRAFT_TICKS
                    val burning = isBurnActive()

                    startButton.disabled = !burning && (discResetting || !(validCost && validOutput))
                    startButton.processing = burning
                    startButton.shouldRepeat = false
                }

                if (::selectedMoveButton.isInitialized) {
                    selectedMoveButton.enabled = (heldStackMove == null || heldStackMove == menu.currentlySelectedTM?.moveName)
                }

                if (isBurnActive()) {
                    val progress = menu.getBurnProgressRatio()
                    if (progress > 0 && progress < 1) {
                        discReflectionFrame = (discReflectionFrame + 1) % 4
                        discRotationFrame = (discRotationFrame + 1) % 8
                    }
                }
            }
        }

        if (heldMoveItemChanged) {
            heldMoveItemChanged = false
            updateHeldStackMove()
        }

        // Screen saver
        if (ticksElapsed > SCREEN_SAVER_TIMEOUT_TICKS) {
            logoPosX = logoPosX + (if (logoSlideRight) 1 else -1)
            logoPosY = logoPosY + (if (logoSlideDown) 1 else -1)
            var borderHit = false

            if (logoSlideRight && (logoPosX + (LOGO_WIDTH * HALF_SCALE).toInt()) >= (leftPos + SCREEN_SAVER_WIDTH + 1)) {
                logoSlideRight = false
                borderHit = true
            }
            if ((!logoSlideRight) && logoPosX <= (leftPos + 1)) {
                logoSlideRight = true
                borderHit = true
            }

            if (logoSlideDown && (logoPosY + (LOGO_HEIGHT * HALF_SCALE).toInt()) >= (topPos + SCREEN_SAVER_HEIGHT + 1)) {
                logoSlideDown = false
                borderHit = true
            }
            if ((!logoSlideDown) && logoPosY <= (topPos + 1)) {
                logoSlideDown = true
                borderHit = true
            }

            if (borderHit) {
                logoTint = ElementalTypes.all().random().secondaryColor.toRGB()
            }
        }

        updateSlotItemMemory()

        super.containerTick()
    }

    private fun resetScreenSaver(): Boolean {
        val wasScreenSaverActive = ticksElapsed > SCREEN_SAVER_TIMEOUT_TICKS
        ticksElapsed = 0
        return wasScreenSaverActive
    }

    private fun isBurnActive(): Boolean = (menu.containerData?.get(TMMachineBlockEntity.BURN_ACTIVE_INDEX) ?: 0) == 1

    private fun setScreenFromMode(screen: Int) {
        mode = screen
        when (mode) {
            TYPE_SELECT_MODE -> {
                toggleTMBurn(false)
                toggleMoveSelect(false)
                toggleTypeSelect(true)
            }
            MOVE_SELECT_MODE -> {
                toggleTMBurn(false)
                toggleTypeSelect(false)
                toggleMoveSelect(true)
            }
            else -> {
                toggleTypeSelect(false)
                toggleMoveSelect(false)
                toggleTMBurn(true)
            }
        }
        updateHeldStackMove()
    }

    private fun setMoveList(filter: String? = null, pokemon: Pokemon? = null) {
        val filteredList = TechnicalMachine.filterTms(
            search = filter,
            type = sortType,
            pokemon = pokemon,
            player = inventory.player,
            includeUnlearned = ServerSettings.unlockAllMoveDexMovesByDefault
        ).toMutableList()
        pokemon?.let { selectedPokemon ->
            filteredList.retainAll { tm -> tm.moveName in selectedPokemon.form.moves.tmMoves }
        }
        if (sortType == null) filteredList.sortBy { it.type }
        tmList.set(filteredList)
    }

    private fun toggleTypeSelect(isVisible: Boolean) {
        if (isVisible) {
            if (::moveSearchWidget.isInitialized) moveSearchWidget.value = ""
            if (selectedPokemon != null) selectedPokemon = null
            sortType = null
        }

        if (::typesScrollList.isInitialized) {
            typesScrollList.apply {
                visible = isVisible
                scrollAmount = 0.0
                // Resize widget to toggle input detection as screen only detects first widget added for overlapping children
                if (isVisible) { setSize(TypesScrollingWidget.WIDTH, TypesScrollingWidget.HEIGHT) }
                else { setSize(0, 0) }
            }
        }

        setPartySlotsClickable(isVisible)
    }

    private fun toggleMoveSelect(isVisible: Boolean) {
        if (::movesScrollingList.isInitialized) {
            movesScrollingList.apply {
                visible = isVisible
                scrollAmount = 0.0
                if (isVisible) { setSize(MovesScrollingWidget.WIDTH, MovesScrollingWidget.HEIGHT) }
                else { setSize(0, 0) }
            }
        }
        if (::moveSearchWidget.isInitialized) {
            moveSearchWidget.visible = isVisible
        }
        if (::backButton.isInitialized) {
            backButton.visible = isVisible
        }
        heldStackMove = null
    }

    private fun toggleTMBurn(isVisible: Boolean) {
        if (::selectedMoveButton.isInitialized) selectedMoveButton.visible = isVisible
        if (::startButton.isInitialized) startButton.visible = isVisible
    }

    fun canLearnTMMove(move: MoveTemplate, pokemon: Pokemon): Int {
        if (pokemon.moveSet.getMoveTemplates().contains(move) || pokemon.allAccessibleMoves.contains(move)) return TMPartySlotWidget.LEARNED
        val learnableMoves = pokemon.form.moves.tmMoves
        return if (learnableMoves.contains(move)) TMPartySlotWidget.CAN_LEARN else TMPartySlotWidget.CANNOT_LEARN
    }

    fun updateSelectedTM(tm: TechnicalMachine?) {
        if (!initScreen) {
            initScreen = true
            tm?.let {
                setScreenFromMode(TM_BURN_MODE)
                sortType = ElementalTypes.get(tm.type)
                setMoveList()
            }
        }

        if (mode == TM_BURN_MODE) updateMoveInformation(tm)

        tm?.let {
            if (::selectedMoveButton.isInitialized) {
                selectedMoveButton.apply {
                    move = it.moveName
                    pp = it.moveName.pp
                }
            }
        }
    }

    fun updateMoveInformation(tm: TechnicalMachine?) {
        moveInfoTM = tm

        if (::moveDescriptionWidget.isInitialized) {
            val selectedDescription = tm?.moveName?.description?.string
            moveDescriptionWidget.setText(
                if (selectedDescription != null) listOf(selectedDescription) else emptyList()
            )
        }

        if (::movesScrollingList.isInitialized) {
            movesScrollingList.setSlotHighlighted(tm?.id)
        }
    }

    fun updatePartySlotStatus(moveTemplate: MoveTemplate?) {
        for (slot in partySlotList) {
            if (slot.pokemon != null) {
                slot.teachable = if (moveTemplate != null) canLearnTMMove(moveTemplate, slot.pokemon) else null
            }
        }
    }

    fun renderDiscTypeOverlay(context: GuiGraphicsExtractor, isDiscRotating: Boolean, alpha: Float = 1F) {
        menu.currentlySelectedTM?.let {
            val primaryRgb = it.moveName.elementalType.primaryColor.toRGB()
            val secondaryRgb = it.moveName.elementalType.secondaryColor.toRGB()
            blitk(context = context,
            texture = discBase,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            red = primaryRgb.first,
            green = primaryRgb.second,
            blue = primaryRgb.third,
            alpha = alpha)

            if (isDiscRotating) {
                // Reflective rotation
                blitk(context = context,
            texture = discReflectiveRotating,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            vOffset = DISC_DIAMETER * discReflectionFrame,
            textureHeight = DISC_DIAMETER * 4,
            red = secondaryRgb.first,
            green = secondaryRgb.second,
            blue = secondaryRgb.third,
            alpha = alpha)

                // Groove rotation
                blitk(context = context,
            texture = discRotating,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            vOffset = DISC_DIAMETER * discRotationFrame,
            textureHeight = DISC_DIAMETER * 8,
            red = secondaryRgb.first,
            green = secondaryRgb.second,
            blue = secondaryRgb.third,
            alpha = alpha)
            } else {
                blitk(context = context,
            texture = discReflective,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            red = secondaryRgb.first,
            green = secondaryRgb.second,
            blue = secondaryRgb.third,
            alpha = alpha)
            }

            val postCraftTicks = menu.getPostCraftTicks()
            if (postCraftTicks > 0 && postCraftTicks < 8) {
                val opacity = when {
                    postCraftTicks <= 5 -> postCraftTicks * 0.25F
                    postCraftTicks > 5 -> (CRAFT_TICKS - postCraftTicks) * 0.25F  // decreases by 0.25 each tick after 5
                    else -> 0F
                }

                blitk(context = context,
            texture = discOverlay,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            alpha = opacity)
            }
        }
    }

    fun renderDisc(context: GuiGraphicsExtractor, isDiscRotating: Boolean, baseAlpha: Float = 1F, overlayAlpha: Float = 1F) {
        blitk(context = context,
            texture = discBase,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            alpha = baseAlpha)

        if (isDiscRotating) {
            // Reflective rotation
            blitk(context = context,
            texture = discReflectiveRotating,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            vOffset = DISC_DIAMETER * discReflectionFrame,
            textureHeight = DISC_DIAMETER * 4,
            alpha = baseAlpha)

            // Groove rotation
            blitk(context = context,
            texture = discRotating,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            vOffset = DISC_DIAMETER * discRotationFrame,
            textureHeight = DISC_DIAMETER * 8,
            alpha = baseAlpha)
        } else {
            blitk(context = context,
            texture = discReflective,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            alpha = baseAlpha)
        }

        if (menu.getPostCraftTicks() < CRAFT_TICKS) {
            renderDiscTypeOverlay(context, isDiscRotating, overlayAlpha)
        }

        blitk(context = context,
            texture = discBorder,
            x = leftPos + 32,
            y = topPos + 50,
            width = DISC_DIAMETER,
            height = DISC_DIAMETER,
            alpha = baseAlpha)
    }

    fun renderMoveInfo(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val moveTemplate = moveInfoTM?.moveName

        blitk(context = context,
            texture = MovesWidget.movesPowerIconResource,
            x = leftPos + 127.5,
            y = topPos + 7.5,
            width = 5,
            height = 5)

        blitk(context = context,
            texture = MovesWidget.movesAccuracyIconResource,
            x = leftPos + 127.5,
            y = topPos + 19.5,
            width = 5,
            height = 5)

        blitk(context = context,
            texture = MovesWidget.movesEffectIconResource,
            x = leftPos + 127.5,
            y = topPos + 31.5,
            width = 5,
            height = 5)

        drawScaledText(
            context = context,
            text = lang("ui.power"),
            x = leftPos + 134.5,
            y = topPos + 8,
            shadow = true,
            scale = HALF_SCALE
        )

        drawScaledText(
            context = context,
            text = lang("ui.accuracy"),
            x = leftPos + 134.5,
            y = topPos + 20,
            shadow = true,
            scale = HALF_SCALE
        )

        drawScaledText(
            context = context,
            text = lang("ui.effect"),
            x = leftPos + 134.5,
            y = topPos + 32,
            shadow = true,
            scale = HALF_SCALE
        )

        val movePower = if (moveTemplate != null && moveTemplate.power.toInt() > 0) moveTemplate.power.toInt().toString().text() else "—".text()
        drawScaledTextJustifiedRight(
            context = context,
            text = movePower,
            x = leftPos + 183,
            y = topPos + 8,
            shadow = true,
            scale = HALF_SCALE
        )

        val moveAccuracy = if (moveTemplate != null) MovesWidget.format(moveTemplate.accuracy).text() else "—".text()
        drawScaledTextJustifiedRight(
            context = context,
            text = moveAccuracy,
            x = leftPos + 183,
            y = topPos + 20,
            shadow = true,
            scale = HALF_SCALE
        )

        val moveEffect = if (moveTemplate != null) MovesWidget.format(moveTemplate.effectChances.firstOrNull() ?: 0.0).text() else "—".text()
        drawScaledTextJustifiedRight(
            context = context,
            text = moveEffect,
            x = leftPos + 183,
            y = topPos + 32,
            shadow = true,
            scale = HALF_SCALE
        )

        if (moveTemplate != null) {
            val recipe = TechnicalMachines.moveToTM[moveTemplate]?.getClampedRecipe() ?: listOf()
            // Render material cost
            recipe.forEachIndexed { index, recipe ->
                val itemX = (leftPos + 129 + (index * 18))
                val itemY = (topPos + 90)

                val stacks = recipe.ingredient.items().map { it.value().defaultInstance }.toList()
                if (stacks.isEmpty()) return@forEachIndexed

                val level = Minecraft.getInstance().level ?: return@forEachIndexed
                val index = ((level.gameTime / 20) % stacks.size).toInt()

                val itemStack = stacks[index].copy().also { it.count = recipe.count }

                context.item(itemStack, itemX, itemY)
                context.itemDecorations(Minecraft.getInstance().font, itemStack, itemX, itemY)

                if (
                    menu.carried.isEmpty &&
                    mouseX >= itemX && mouseX < itemX + 16 &&
                    mouseY >= itemY && mouseY < itemY + 16
                ) {
                    context.setTooltipForNextFrame(Minecraft.getInstance().font, itemStack, mouseX, mouseY)
                }
            }
        }
    }

    fun renderHeldMoveInfoTooltip(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        heldStackMove?.let { moveTemplate ->
            val matrices = context.pose()
            val posX = mouseX + 10
            val posY = mouseY - 49

            matrices.pushMatrix()
            context.nextStratum()
            matrices.translate((0.0F).toFloat(), (0.0F).toFloat())
            blitk(context = context,
            texture = tooltipMoveInfo,
            x = posX,
            y = posY,
            width = 96,
            height = 98,
            alpha = 0.96F)

            drawScaledText(
                context = context,
                text = moveTemplate.displayName.bold(),
                x = posX + 6,
                y = posY + 6,
                shadow = true,
                scale = HALF_SCALE
            )

            TypeIcon(posX + 81, posY + 3.5, moveTemplate.elementalType, small = true).render(context)

            blitk(context = context,
            texture = MovesWidget.movesPowerIconResource,
            x = posX + 8.5,
            y = posY + 15.5,
            width = 5,
            height = 5)

            blitk(context = context,
            texture = MovesWidget.movesAccuracyIconResource,
            x = posX + 8.5,
            y = posY + 27.5,
            width = 5,
            height = 5)

            blitk(context = context,
            texture = MovesWidget.movesEffectIconResource,
            x = posX + 8.5,
            y = posY + 39.5,
            width = 5,
            height = 5)

            drawScaledText(
                context = context,
                text = lang("ui.power"),
                x = posX + 15.5,
                y = posY + 16,
                shadow = true,
                scale = HALF_SCALE
            )

            drawScaledText(
                context = context,
                text = lang("ui.accuracy"),
                x = posX + 15.5,
                y = posY + 28,
                shadow = true,
                scale = HALF_SCALE
            )

            drawScaledText(
                context = context,
                text = lang("ui.effect"),
                x = posX + 15.5,
                y = posY + 40,
                shadow = true,
                scale = HALF_SCALE
            )

            val movePower = if (moveTemplate.power.toInt() > 0) moveTemplate.power.toInt().toString().text() else "—".text()
            drawScaledTextJustifiedRight(
                context = context,
                text = movePower,
                x = posX + 88,
                y = posY + 16,
                shadow = true,
                scale = HALF_SCALE
            )

            val moveAccuracy = MovesWidget.format(moveTemplate.accuracy).text()
            drawScaledTextJustifiedRight(
                context = context,
                text = moveAccuracy,
                x = posX + 88,
                y = posY + 28,
                shadow = true,
                scale = HALF_SCALE
            )

            val moveEffect = MovesWidget.format(moveTemplate.effectChances.firstOrNull() ?: 0.0).text()
            drawScaledTextJustifiedRight(
                context = context,
                text = moveEffect,
                x = posX + 88,
                y = posY + 40,
                shadow = true,
                scale = HALF_SCALE
            )

            matrices.pushMatrix()
            matrices.scale((HALF_SCALE).toFloat(), (HALF_SCALE).toFloat())
            MultiLineLabelK.create(
                component = moveTemplate.description,
                width = 79 / HALF_SCALE,
                maxLines = 5
            ).renderLeftAligned(
                context = context,
                x = (posX + 8.5) / HALF_SCALE,
                y = (posY + 52.5) / HALF_SCALE,
                ySpacing = 6 / HALF_SCALE,
                colour = ColourLibrary.WHITE,
                shadow = true
            )
            matrices.popMatrix()

            drawScaledText(
                context = context,
                text = lang("ui.moves.pp", moveTemplate.pp).bold(),
                x = posX + 6,
                y = posY + 88,
                scale = HALF_SCALE,
                shadow = true
            )
            MoveCategoryIcon(x = posX + 78, y = posY + 86, category = moveTemplate.damageCategory).render(context)

            matrices.popMatrix()
        }
    }

    fun renderScreenSaver(context: GuiGraphicsExtractor) {
        val poseStack = context.pose()
        // Screen saver
        if (ticksElapsed > SCREEN_SAVER_TIMEOUT_TICKS) {
            val opacity = Math.max(0.0, Math.min(1.0, (ticksElapsed - SCREEN_SAVER_TIMEOUT_TICKS) * 0.2))
            poseStack.pushMatrix()
            context.nextStratum()
            poseStack.translate((0.0).toFloat(), (0.0).toFloat())
            blitk(context = context,
            texture = screenSaver,
            x = leftPos + 1,
            y = topPos + 1,
            width = SCREEN_SAVER_WIDTH,
            height = SCREEN_SAVER_HEIGHT,
            alpha = opacity)

            blitk(context = context,
            texture = logoResource,
            x = logoPosX / HALF_SCALE,
            y = logoPosY / HALF_SCALE,
            width = LOGO_WIDTH,
            height = LOGO_HEIGHT,
            scale = HALF_SCALE,
            red = logoTint.first,
            green = logoTint.second,
            blue = logoTint.third,
            alpha = 0.8 * opacity)

            blitk(context = context,
            texture = cobblemonResource("textures/white.png"),
            x = leftPos + 1,
            y = scanPosY * 0.5,
            width = SCREEN_SAVER_WIDTH,
            height = 1,
            red = 0,
            green = 0,
            blue = 0,
            alpha = 0.1 * opacity)

            context.enableScissor(
                leftPos + 1,
                topPos + 1,
                leftPos + 1 + SCREEN_SAVER_WIDTH,
                topPos + 1 + SCREEN_SAVER_HEIGHT
            )
            blitk(context = context,
            texture = scanLines,
            x = leftPos + 1,
            y = topPos + 1 - scanLineOffsetY,
            width = 118,
            height = 114,
            alpha = opacity)
            context.disableScissor()

            blitk(context = context,
            texture = screenOverlay,
            x = leftPos + 1,
            y = topPos + 1,
            width = SCREEN_SAVER_WIDTH,
            height = SCREEN_SAVER_HEIGHT,
            alpha = 0.5 * opacity)

            poseStack.popMatrix()
        }
    }

    private fun setPartySlotsClickable(clickable: Boolean) {
        for (slot in partySlotList) slot.clickable = clickable
    }

    private fun updateHeldStackMove() {
        val heldStack = menu.carried
        val heldStackIsTm = heldStack.item == CobblemonItems.TECHNICAL_MACHINE

        if (heldStackIsTm) {
            // Store move of TM item picked up by player cursor
            TMMoveComponent.getTMMove(heldStack)?.let { heldMove ->
                if (heldStackMove != heldMove) heldStackMove = heldMove
            }

            if (::movesScrollingList.isInitialized) {
                movesScrollingList.setDisabled(true, TechnicalMachines.moveToTM[heldStackMove])
                if (mode == MOVE_SELECT_MODE) {
                    val heldStackTm = TechnicalMachines.moveToTM[heldStackMove]
                    updateMoveInformation(if (tmList.get().contains(heldStackTm)) heldStackTm else null)

                    // Scroll to move if moves list contains held TM move
                    if (tmList.get().contains(heldStackTm)) {
                        val moveListIndex = tmList.get().indexOf(heldStackTm)
                        movesScrollingList.scrollAmount = (if (moveListIndex == tmList.get().lastIndex) 1.0 else (moveListIndex / tmList.get().size.toDouble()))* movesScrollingList.maxScrollAmount()
                    }
                }
            }

            if (::typesScrollList.isInitialized) {
                typesScrollList.setDisabled(true, heldStackMove?.elementalType)
            }

            updatePartySlotStatus(heldStackMove)
        } else if (heldStackMove != null) {
            // Reset only if last held item was a TM
            heldStackMove = null

            // Reset selected move on move select screen
            if (mode == MOVE_SELECT_MODE) updateMoveInformation(null)
            updatePartySlotStatus(menu.currentlySelectedTM?.moveName)

            if (::movesScrollingList.isInitialized) {
                movesScrollingList.setDisabled(false)
            }
            if (::typesScrollList.isInitialized) {
                typesScrollList.setDisabled(false)
            }
        }

        setPartySlotsClickable(mode == TYPE_SELECT_MODE || heldStackIsTm)
    }

    private fun checkForSlotChanges() {
        if (menu.inventory == null) return

        val blankTmSlotItem = menu.inventory!!.getItem(TMMachineMenu.BLANK_TM_SLOT).item
        if (oldBlankTmSlotItem != blankTmSlotItem) {
            playSound(if (blankTmSlotItem == Items.AIR) CobblemonSounds.TM_MACHINE_TM_RETRIEVE else CobblemonSounds.TM_MACHINE_TM_INSERT)
        }

        TMMachineMenu.INGREDIENT_SLOTS.forEachIndexed { index, slot ->
            val ingredientItem = menu.inventory!!.getItem(slot).item
            if (oldIngredientItems[index] != ingredientItem) {
                playSound(if (ingredientItem == Items.AIR) CobblemonSounds.TM_MACHINE_ITEM_RETRIEVE else CobblemonSounds.TM_MACHINE_ITEM_INSERT)
            }
        }

        val resultTmSlotItem = menu.inventory!!.getItem(TMMachineMenu.RESULT_SLOT).item
        if (oldResultTmSlotItem != resultTmSlotItem) {
            if (resultTmSlotItem == Items.AIR) playSound(CobblemonSounds.TM_MACHINE_TM_RETRIEVE)
        }

        updateSlotItemMemory()
    }

    private fun updateSlotItemMemory() {
        if (menu.inventory == null) return

        oldBlankTmSlotItem = menu.inventory!!.getItem(TMMachineMenu.BLANK_TM_SLOT).item
        TMMachineMenu.INGREDIENT_SLOTS.forEachIndexed { index, slot ->
            oldIngredientItems[index] = menu.inventory!!.getItem(slot).item
        }
        oldResultTmSlotItem = menu.inventory!!.getItem(TMMachineMenu.RESULT_SLOT).item
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        ticksElapsed = 0
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        ticksElapsed = 0
        if (resetScreenSaver()) return false
        return super.mouseClicked(event, doubleClick)
    }

    override fun slotClicked(slot: Slot?, slotId: Int, mouseButton: Int, type: ContainerInput) {
        super.slotClicked(slot, slotId, mouseButton, type)
        updateHeldStackMove()
        checkForSlotChanges()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        ticksElapsed = 0
        if (resetScreenSaver()) return false
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.key()
        ticksElapsed = 0
        if (resetScreenSaver()) return false
        val searchFocused = this::moveSearchWidget.isInitialized && moveSearchWidget.isFocused

        if (isInventoryKeyPressed(minecraft, event)) {
            if (searchFocused) { return true }
            else {
                onClose()
                return true
            }
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (searchFocused) {
                // Escape from text box
                this.focused = null
                return true
            } else {
                onClose()
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun extractBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {}


    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractTransparentBackground(graphics)

        val matrices = graphics.pose()
        blitk(context = graphics,
            texture = baseResource,
            x = leftPos,
            y = topPos,
            width = WIDTH,
            height = HEIGHT)

        blitk(context = graphics,
            texture = emptyDiscSlot,
            x = leftPos + 105,
            y = topPos + 116,
            width = 16,
            height = 16)

        var label = if (mode != MOVE_SELECT_MODE) "block.cobblemon.tm_machine".asTranslated()
            else ( if (selectedPokemon != null) selectedPokemon!!.getDisplayName() else (if (sortType != null) lang("type.suffix", sortType!!.displayName) else lang("ui.moves")))
        drawScaledText(
            context = graphics,
            font = CobblemonResources.DEFAULT_LARGE,
            text = label.bold(),
            x = leftPos + 5,
            y = topPos + 1
        )

        if (mode == TM_BURN_MODE) {
            blitk(context = graphics,
            texture = tmTray,
            x = leftPos,
            y = topPos,
            width = 120,
            height = 112)

            val burnProgressWidth = ceil(menu.getBurnProgressRatio() * 81).toInt()
            blitk(context = graphics,
            texture = CobblemonResources.WHITE,
            x = leftPos + 33,
            y = topPos + 36,
            width = burnProgressWidth,
            height = 2)

            if (!menu.inventory!!.getItem(TMMachineMenu.BLANK_TM_SLOT).isEmpty) {
                val postCraftTicks = menu.getPostCraftTicks()
                val baseOpacity = when {
                    ((postCraftTicks >= (CRAFT_TICKS + 1)) && (postCraftTicks <= (CRAFT_TICKS + RESET_DISC_TICKS))) -> (postCraftTicks - (CRAFT_TICKS + 1)) * 0.25F
                    postCraftTicks == (CRAFT_TICKS) -> 0F
                    else -> 1F
                }

                val overlayOpacity = if (postCraftTicks < CRAFT_TICKS) menu.getBurnProgressRatio() else 0F
                renderDisc(graphics, (menu.getBurnProgressRatio() < 1F) && isBurnActive(), baseOpacity, overlayOpacity)
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)

        if (mode == TM_BURN_MODE && ::selectedMoveButton.isInitialized) {
            selectedMoveButton.showOverlayBar = !selectedMoveButton.isHovered

            if (selectedMoveButton.isHovered) {
                blitk(context = graphics,
            texture = moveOverlayBar,
            x = selectedMoveButton.x + 85,
            y = selectedMoveButton.y + 13,
            width = 22,
            height = 8)

                blitk(context = graphics,
            texture = iconBack,
            x = (selectedMoveButton.x + 92) / HALF_SCALE,
            y = (selectedMoveButton.y + 13) / HALF_SCALE,
            width = 20,
            height = 18,
            textureHeight = 36,
            vOffset = 18,
            scale = HALF_SCALE)
            }
        }

        renderScreenSaver(graphics)

        renderMoveInfo(graphics, mouseX, mouseY)

        this.extractTooltip(graphics, mouseX, mouseY)

        renderHeldMoveInfoTooltip(graphics, mouseX, mouseY)
    }

    override fun extractBlurredBackground(context: GuiGraphicsExtractor) {}
    override fun extractLabels(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {}

    fun playSound(soundEvent: SoundEvent) {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(soundEvent, 1.0F))
    }

    override fun hasClickedOutside(
        mouseX: Double,
        mouseY: Double,
        guiLeft: Int,
        guiTop: Int
    ): Boolean {
        partySlotList.forEach {
            if (it.pokemon != null && it.isMouseOver(mouseX, mouseY)) return false
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)
    }

    class MoveDescriptionWidget(pX: Int, pY: Int, width: Int, height: Int) : ScrollingWidget<MoveDescriptionWidget.TextSlot>(
        left = pX,
        top = pY - height,
        width = width,
        height = height,
        slotHeight = 6
    ) {
        fun setText(text: Collection<String>) {
            clearEntries()
            text.forEach { line ->
                Minecraft.getInstance().font.splitter.splitLines(
                    Component.literal(line),
                    ((width - DESCRIPTION_SCROLLBAR_WIDTH - 5) / HALF_SCALE).toInt(),
                    Style.EMPTY
                ).stream()
                    .map { it.string }
                    .forEach { addEntry(TextSlot(it)) }
            }
            scrollAmount = 0.0
        }

        override fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            val xLeft = this.scrollBarX()

            val barHeight = this.bottom - y

            var yBottom = ((barHeight * barHeight).toFloat() / (this.contentHeight() - 4).toFloat()).toInt()
            yBottom = Mth.clamp(yBottom, 16, barHeight - 6)
            var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScrollAmount() + y
            if (yTop < y) yTop = y

            // Scroll Track
            blitk(texture = scrollbarTrack,
            context = context,
            x = xLeft,
            y = y,
            width = 2,
            height = height)

            // Scroll Slide
            blitk(texture = scrollbarSlide,
            context = context,
            x = xLeft,
            y = yTop,
            width = 2,
            height = yBottom)
        }

        override fun scrollBarX(): Int {
            return left + width - DESCRIPTION_SCROLLBAR_WIDTH
        }

        class TextSlot(val text: String) : Slot<TextSlot>() {
            override fun extractContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
                val y = this.contentY
                val x = this.contentX
                drawScaledText(
                    context = context,
                    text = text.text(),
                    x = x + 2.5,
                    y = y + 2.5,
                    scale = HALF_SCALE,
                    shadow = true
                )
            }

            override fun getNarration(): Component {
                return Component.literal(text)
            }
        }
    }
}
