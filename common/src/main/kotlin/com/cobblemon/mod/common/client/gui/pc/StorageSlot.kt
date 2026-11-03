/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pc

import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.pasture.PasturePCGUIConfiguration
import com.cobblemon.mod.common.client.gui.PokemonGUIAnimationStyle
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import org.joml.Quaternionf
import org.joml.Vector3f

open class StorageSlot(
    x: Int, y: Int,
    private val parent: StorageWidget,
    onPress: OnPress
) : Button(x, y, SIZE, SIZE, Component.literal("StorageSlot"), onPress, DEFAULT_NARRATION), CobblemonRenderable {
    val state = FloatingState()

    companion object {
        const val SIZE = 25

        private val genderIconMale = cobblemonResource("textures/gui/pc/gender_icon_male.png")
        private val genderIconFemale = cobblemonResource("textures/gui/pc/gender_icon_female.png")
        private val selectPointerResource = cobblemonResource("textures/gui/pc/pc_pointer.png")
        private val slotOverlayResource = cobblemonResource("textures/gui/pc/pc_slot_overlay.png")
        private val slotOverlayPastureIconResource = cobblemonResource("textures/gui/pasture/pc_slot_icon_pasture.png")
        private val slotOverlayMoveIconResource = cobblemonResource("textures/gui/pasture/pc_slot_icon_move.png")
    }

    protected var isSlotSelected = false;

    override fun playDownSound(soundManager: SoundManager) {
    }

    override fun extractContents(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (shouldRender()) {
            renderSlot(context, x, y, delta)
        }
    }

    fun renderSlot(context: GuiGraphicsExtractor, posX: Int, posY: Int, partialTicks: Float) {
        val pokemon = getPokemon() ?: return
        val matrices = context.pose()
        context.enableScissor(
            posX - 2,
            posY + 2,
            posX + SIZE + 4,
            posY + SIZE + 4
        )

        // Render Pokémon
        matrices.pushMatrix()
        matrices.translate((posX + (SIZE / 2.0)).toFloat(), (posY + 1.0).toFloat())
        matrices.scale((2.5F).toFloat(), (2.5F).toFloat())

        val animationConfig = if (this is PartyStorageSlot) Cobblemon.config.summaryProfileAnimations else Cobblemon.config.pcProfileAnimations
        val shouldAnimate = when (animationConfig) {
            PokemonGUIAnimationStyle.ALWAYS_ANIMATE -> true
            PokemonGUIAnimationStyle.NEVER_ANIMATE -> false
            PokemonGUIAnimationStyle.ANIMATE_SELECTED -> isHoveredOrFocused
        }

        drawProfilePokemon(renderablePokemon = pokemon.asRenderablePokemon(),
                context = context,
                rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                state = state,
                partialTicks = if (shouldAnimate) partialTicks else 0F,
                scale = 4.5F)
        matrices.popMatrix()

        context.disableScissor()

        if (!isSlotSelected) {
            // Ensure elements are not hidden behind Pokémon render
            matrices.pushMatrix()
            context.nextStratum()
            matrices.translate((0.0).toFloat(), (0.0).toFloat())
            // Level
            drawScaledText(
                context = context,
                text = lang("ui.lv.number", pokemon.level),
                x = posX + 1,
                y = posY + 1,
                shadow = true,
                scale = PCGUI.SCALE
            )

            if (pokemon.gender != Gender.GENDERLESS) {
                blitk(context = context,
            texture = if (pokemon.gender == Gender.MALE) genderIconMale else genderIconFemale,
            x = (posX + 21) / PCGUI.SCALE,
            y = (posY + 1) / PCGUI.SCALE,
            width = 6,
            height = 8,
            scale = PCGUI.SCALE)
            }

            // Held Item
            val heldItem = pokemon.heldItemNoCopy()
            if (!heldItem.isEmpty) {
                renderScaledGuiItemIcon(
                    itemStack = heldItem,
                    x = posX + 16.0,
                    y = posY + 16.0,
                    scale = 0.5,
                    context = context
                )
            }
            matrices.popMatrix()
        }

        // Ensure overlay elements are on top
        matrices.pushMatrix()
        context.nextStratum()
        matrices.translate((0.0).toFloat(), (0.0).toFloat())

        val config = parent.pcGui.configuration
        if (pokemon.tetheringId != null && !isSlotSelected) {
            if (isStationary()) {
                blitk(context = context,
            x = posX,
            y = posY,
            width = SIZE,
            height = SIZE,
            texture = slotOverlayResource)
            }

            val opacity = if (config is PasturePCGUIConfiguration && config.pasturedPokemon.get().none { it.pokemonId == pokemon.uuid }) 0.5F else 1F

            blitk(context = context,
            x = (posX + 7.5) / PCGUI.SCALE,
            y = (posY + 7.5) / PCGUI.SCALE,
            width = 20,
            height = 20,
            texture = slotOverlayPastureIconResource,
            scale = PCGUI.SCALE,
            alpha = opacity)
        }

        if (isHoveredOrFocused) {
            // If pasture UI and slot is not in pasture
            if (config is PasturePCGUIConfiguration
                && pokemon.tetheringId == null
                && isStationary()
                && config.permissions.canPasture
                && config.canSelect(pokemon)
                && config.pasturedPokemon.get().size < config.limit
                && config.pasturedPokemon.get().count { it.playerId == Minecraft.getInstance().player!!.uuid } < config.permissions.maxPokemon
            ) {
                blitk(context = context,
            x = posX,
            y = posY,
            width = SIZE,
            height = SIZE,
            texture = slotOverlayResource)

                blitk(context = context,
            x = (posX + 7.5) / PCGUI.SCALE,
            y = (posY + 7.5) / PCGUI.SCALE,
            width = 20,
            height = 20,
            texture = slotOverlayMoveIconResource,
            scale = PCGUI.SCALE)
            }

            // Arrow pointer
            blitk(context = context,
            texture = selectPointerResource,
            x = (posX + 10) / PCGUI.SCALE,
            y = ((posY - 3) / PCGUI.SCALE) - parent.pcGui.selectPointerOffsetY,
            width = 11,
            height = 8,
            scale = PCGUI.SCALE)
        }
        matrices.popMatrix()
    }

    open fun isStationary(): Boolean {
        return true
    }

    open fun getPokemon(): Pokemon? {
        return null
    }

    override fun isHoveredOrFocused(): Boolean {
        return getPokemon() == parent.pcGui.previewPokemon
    }

    open fun shouldRender(): Boolean {
        return parent.pcGui.search.passes(getPokemon())
    }

    open fun clickable(): Boolean {
        return getPokemon() == null || parent.pcGui.search.passes(getPokemon())
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        return if (clickable()) super.mouseClicked(event, doubleClick) else false
    }

    fun isHovered(mouseX: Int, mouseY: Int) = mouseX.toFloat() in (x.toFloat()..(x.toFloat() + SIZE)) && mouseY.toFloat() in (y.toFloat()..(y.toFloat() + SIZE))
}