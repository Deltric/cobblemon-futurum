/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.scrollAmount
import net.minecraft.client.input.MouseButtonEvent
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.ClientMoLangFunctions.setupClient
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_BAR_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_BASE_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_SLOT_SIZE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_SLOT_SPACING
import com.cobblemon.mod.common.client.gui.pokedex.widgets.EntriesScrollingWidget.PokemonScrollSlotRow
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import org.joml.Quaternionf
import org.joml.Vector3f

class EntriesScrollingWidget(val pX: Int, val pY: Int, val setPokedexEntry: (PokedexEntry) -> (Unit)): ScrollingWidget<PokemonScrollSlotRow>(
    width = PokedexGUIConstants.HALF_OVERLAY_WIDTH,
    height = SCROLL_BASE_HEIGHT,
    left = pX,
    top = pY - SCROLL_BASE_HEIGHT,
    slotHeight = SCROLL_SLOT_SIZE + 2
) {
    companion object {
        val scrollbarSlide = cobblemonResource("textures/gui/pokedex/scrollbar_slide.png")
        val scrollbarTrack = cobblemonResource("textures/gui/pokedex/scrollbar_track.png")
    }

    fun createEntries(filteredPokedex: Collection<PokedexEntry>) {
        filteredPokedex.chunked(5).forEachIndexed { index, listChunk ->
            val discoveryList = listChunk.map { CobblemonClient.clientPokedexData.getHighestKnowledgeFor(it) }.toMutableList()
            val newEntry = PokemonScrollSlotRow(
                listChunk.toMutableList(),
                discoveryList,
                setPokedexEntry
            )
            addEntry(newEntry)
        }
    }

    override fun addEntry(entry: PokemonScrollSlotRow): Int {
        return super.addEntry(entry)
    }

    override fun scrollBarX(): Int {
        return pX + width - SCROLL_BAR_WIDTH
    }

    override fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val xLeft = this.scrollBarX()
        val yMargin = 3
        val yStart = y + yMargin

        val barHeight = this.bottom - yMargin - yStart

        var yBottom = ((barHeight * barHeight).toFloat() / (this.contentHeight() - 4).toFloat()).toInt()
        yBottom = Mth.clamp(yBottom, 32, barHeight - 8)
        var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScrollAmount() + yStart
        if (yTop < yStart) yTop = yStart

        // Scroll Track
        blitk(texture = scrollbarTrack,
            context = context,
            x = xLeft,
            y = yStart,
            width = SCROLL_BAR_WIDTH,
            height = height - (yMargin * 2))

        // Scroll Slide
        blitk(texture = scrollbarSlide,
            context = context,
            x = xLeft,
            y = yTop,
            width = SCROLL_BAR_WIDTH,
            height = yBottom)
    }

    override fun extractEntry(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        index: Int,
        x: Int,
        y: Int,
        entryWidth: Int,
        entryHeight: Int
    ) {
        val entry =  this.getEntry(index)
        entry.x = x
        entry.y = y
        entry.x = x
        entry.y = y
        entry.width = entryWidth
        entry.height = entryHeight
        entry.extractContent(context, mouseX, mouseY, focused == entry, delta)
    }

    override fun getEntry(index: Int): PokemonScrollSlotRow {
        return children()[index] as PokemonScrollSlotRow
    }

    class PokemonScrollSlotRow(
        val dexDataList:  MutableList<PokedexEntry>,
        val discoveryLevelList: MutableList<PokedexEntryProgress>,
        val setPokedexEntry : (PokedexEntry) -> (Unit)
    ): Slot<PokemonScrollSlotRow>() {
        companion object {
            private val slotResource = cobblemonResource("textures/gui/pokedex/pokedex_slot.png")
            private val slotHighlight = cobblemonResource("textures/gui/pokedex/slot_select.png")
            private val caughtIcon = cobblemonResource("textures/gui/pokedex/caught_icon_small.png")
            private val unknownIcon = cobblemonResource("textures/gui/pokedex/pokedex_slot_unknown.png")
            private val unimplementedIcon = cobblemonResource("textures/gui/pokedex/pokedex_slot_unimplemented.png")
        }
        val runtime = MoLangRuntime().setupClient().setup().also {
            it.environment.query.addFunction("get_pokedex") { CobblemonClient.clientPokedexData.struct }
        }


        override fun extractContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            val y = this.contentY
            val x = this.contentX
            dexDataList.forEachIndexed { index, dexData ->
                val state = FloatingState()
                val species = PokemonSpecies.getByIdentifier(dexData.speciesId)
                //FIXME: This may not work properly when accounting for custom pokemon with the same dex number
                val pokemonNumber = (species?.nationalPokedexNumber?.toString() ?: "0").padStart(4, '0')

                val speciesNumber = pokemonNumber.text()
                val discoveryLevel = discoveryLevelList[index]
                val firstVisibleForm = CobblemonClient.clientPokedexData.getEncounteredForms(dexData).firstOrNull()
                val shouldDrawMon = firstVisibleForm != null

                if (species == null) {
                    return@forEachIndexed
                }

                val matrices = context.pose()

                val startPosX = x + ((SCROLL_SLOT_SPACING + SCROLL_SLOT_SIZE) * index)
                val startPosY = y + SCROLL_SLOT_SPACING + 1

                blitk(context = context,
            texture = slotResource,
            x = startPosX,
            y = startPosY,
            width = SCROLL_SLOT_SIZE,
            height = SCROLL_SLOT_SIZE)

                if (getHoveredSlotIndex(mouseX, mouseY) == index) {
                    blitk(context = context,
            texture = slotHighlight,
            x = startPosX,
            y = startPosY,
            width = SCROLL_SLOT_SIZE,
            height = SCROLL_SLOT_SIZE,
            vOffset = SCROLL_SLOT_SIZE,
            textureHeight = SCROLL_SLOT_SIZE * 2)
                }

                if (shouldDrawMon) {
                    val firstVisibleGender = CobblemonClient.clientPokedexData.getSeenGenders(dexData, firstVisibleForm).firstOrNull()
                    val shinyStates = CobblemonClient.clientPokedexData.getSeenShinyStates(dexData, firstVisibleForm)
                    val firstVisibleShiny = shinyStates.count() == 1 && shinyStates.first() == "shiny"
                    val formAspects = species.forms.firstOrNull { it.name.equals(firstVisibleForm.displayForm, ignoreCase = true) }?.aspects ?: species.standardForm.aspects
                    val seenAspects = CobblemonClient.clientPokedexData.getSeenAspects(dexData)
                    val variationAspects = dexData.variations.mapNotNull { it.aspects.firstOrNull { it in seenAspects } }
                    context.enableScissor(
                        startPosX + 1,
                        startPosY + 1,
                        startPosX + SCROLL_SLOT_SIZE - 1,
                        startPosY + SCROLL_SLOT_SIZE - 2
                    )
                    val aspectsToDraw = (dexData.displayAspects + variationAspects + formAspects + (firstVisibleGender ?: Gender.GENDERLESS).name.lowercase()).toMutableSet()
                    if (firstVisibleShiny) {
                        aspectsToDraw.add("shiny")
                    }
                    state.currentAspects = aspectsToDraw
                    matrices.pushMatrix()
                    matrices.translate((startPosX + (SCROLL_SLOT_SIZE / 2.0)).toFloat(), (startPosY + 1.0).toFloat())
                    matrices.scale((2.5F).toFloat(), (2.5F).toFloat())
                    drawProfilePokemon(renderablePokemon = RenderablePokemon(species, aspectsToDraw),
                context = context,
                rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                state = state,
                partialTicks = 0F,
                scale = 4.5F)
                    matrices.popMatrix()
                    context.disableScissor()
                } else {
                    blitk(context = context,
            texture = unknownIcon,
            x = startPosX + 8.5,
            y = startPosY + 9,
            width = 8,
            height = 10)

                    if (!species.implemented) {
                        blitk(context = context,
            texture = unimplementedIcon,
            x = (startPosX + 14) / SCALE,
            y = (startPosY + 15.5) / SCALE,
            width = 7,
            height = 7,
            scale = SCALE)
                    }
                }

                // Ensure elements are not hidden behind Pokémon render
                matrices.pushMatrix()
                context.nextStratum()
                matrices.translate((0.0).toFloat(), (0.0).toFloat())

                drawScaledText(
                    context = context,
                    text = speciesNumber,
                    x = startPosX + 1.5,//2,
                    y = startPosY + 2.5,//2
                    shadow = true,
                    scale = SCALE
                )

                if (discoveryLevel == PokedexEntryProgress.OWNED) {
                    blitk(context = context,
            texture = caughtIcon,
            x = (startPosX + 18) / SCALE,
            y = (startPosY + 1.5) / SCALE,
            width = 11,
            height = 11,
            scale = SCALE)
                }

                matrices.popMatrix()
            }
        }

        override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
            val mouseX = event.x()
            val mouseY = event.y()
            val hoverIndex = getHoveredSlotIndex(mouseX.toInt(), mouseY.toInt())
            if (hoverIndex > -1 && hoverIndex < dexDataList.size) {
                setPokedexEntry.invoke(dexDataList[hoverIndex])
                Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.POKEDEX_CLICK, 1.0F))
            }

            return true
        }

        private fun getHoveredSlotIndex(mouseX: Int, mouseY: Int): Int {
            dexDataList.forEachIndexed { index, _ ->
                val startPosX = x + ((SCROLL_SLOT_SPACING + SCROLL_SLOT_SIZE) * index)
                val startPosY = y + SCROLL_SLOT_SPACING + 1

                if (mouseX in startPosX..(startPosX + SCROLL_SLOT_SIZE)
                    && mouseY in startPosY..(startPosY + SCROLL_SLOT_SIZE)) {
                    return index
                }
            }
            return -1
        }

        override fun getNarration(): Component {
            if (dexDataList.isNotEmpty()) {
                return "${dexDataList[0]}-${dexDataList[dexDataList.size - 1]}".text()
            }
            return "".text()
        }
    }
}