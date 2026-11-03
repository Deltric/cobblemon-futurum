/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import com.cobblemon.mod.common.api.gui.renderSprite
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.util.math.toEulerXYZDegrees
import com.cobblemon.mod.common.util.toHex
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.util.LightCoordsUtil
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.rendertype.RenderTypes
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.ModelPreviewRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.world.item.ItemStack
import com.cobblemon.mod.common.client.render.item.HeldItemRenderer
import org.joml.Quaternionf
import kotlin.math.atan

enum class ProfileTransformType {
    PROFILE,
    NONE,
    POKEDEX,
    SUMMARY,
}

fun drawProfilePokemon(
    renderablePokemon: RenderablePokemon,
    context: GuiGraphicsExtractor,
    rotation: Quaternionf,
    poseType: PoseType = PoseType.PROFILE,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F,
    profileTransformType: ProfileTransformType = ProfileTransformType.PROFILE,
    applyBaseScale: Boolean = false,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F,
    headYaw: Float = 0f,
    headPitch: Float = 0f,
    blockLight: Int = 13,
    renderHeldItem: Boolean = false
) = drawProfilePokemon(
    species = renderablePokemon.species.resourceIdentifier,
    context = context,
    rotation = rotation,
    poseType = poseType,
    state = state.also { it.currentAspects = renderablePokemon.aspects },
    partialTicks = partialTicks,
    scale = scale,
    profileTransformType = profileTransformType,
    applyBaseScale = applyBaseScale,
    r = r,
    g = g,
    b = b,
    a = a,
    headYaw = headYaw,
    headPitch = headPitch,
    blockLight = blockLight,
    heldItem = if (renderHeldItem) renderablePokemon.heldItem else null
)

fun drawProfilePokemon(
    species: Identifier,
    context: GuiGraphicsExtractor,
    rotation: Quaternionf,
    poseType: PoseType = PoseType.PROFILE,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F,
    profileTransformType: ProfileTransformType = ProfileTransformType.PROFILE,
    applyBaseScale: Boolean = false,
    doQuirks: Boolean = false,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F,
    headYaw: Float = 0f,
    headPitch: Float = 0f,
    blockLight: Int = 13,
    heldItem: ItemStack? = null
) {
    val matrixStack = PoseStack()
    val geometry = ModelGeometry.Builder()
    matrixStack.scale(scale, scale, -scale)

    val sprite = VaryingModelRepository.getSprite(species, state, SpriteType.PROFILE)

    if (sprite == null) {

        val model = VaryingModelRepository.getPoser(species, state)
        val texture = VaryingModelRepository.getTexture(species, state)

        val renderContext = RenderContext()
        model.context = renderContext
        VaryingModelRepository.getTextureNoSubstitute(species, state).let { renderContext.put(RenderContext.TEXTURE, it) }
        val baseScale = PokemonSpecies.getByIdentifier(species)!!.getForm(state.currentAspects).baseScale
        renderContext.put(RenderContext.SCALE, baseScale)
        renderContext.put(RenderContext.SPECIES, species)
        renderContext.put(RenderContext.ASPECTS, state.currentAspects)
        renderContext.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
        renderContext.put(RenderContext.POSABLE_STATE, state)
        renderContext.put(RenderContext.DO_QUIRKS, doQuirks)

        state.currentModel = model

        val renderType = RenderTypes.entityCutoutCull(texture)

        state.setPoseToFirstSuitable(poseType)
        state.updatePartialTicks(partialTicks)

        if (profileTransformType == ProfileTransformType.NONE) {
            matrixStack.translate(0F, 0F, -4.0F)
            if (applyBaseScale) {
                matrixStack.scale(baseScale, baseScale, 1 / baseScale)
            }
        } else {
            var profileScale: Float
            var profileTranslation: Vec3

            when (profileTransformType) {
                ProfileTransformType.POKEDEX -> {
                    profileScale = model.profilePokedexScale ?: model.profileScale
                    profileTranslation = model.profilePokedexTranslation ?: model.profileTranslation
                }
                ProfileTransformType.SUMMARY -> {
                    profileScale = model.profileSummaryScale ?: model.profileScale
                    profileTranslation = model.profileSummaryTranslation ?: model.profileTranslation
                }
                else -> {
                    profileScale = model.profileScale
                    profileTranslation = model.profileTranslation
                }
            }

            matrixStack.translate(
                profileTranslation.x,
                profileTranslation.y + 1.5 * profileScale,
                profileTranslation.z - 4.0
            )
            matrixStack.scale(profileScale, profileScale, 1 / profileScale)
        }

        matrixStack.rotate(rotation)

        model.applyAnimations(null, state, 0F, 0F, 0F, headYaw, headPitch)

        val buffer = geometry.getBuffer(renderType)
        val packedLight = LightCoordsUtil.pack(blockLight, 0)

        val colour = toHex(r, g, b, a)
        model.withLayerContext(geometry, state, VaryingModelRepository.getLayers(species, state)) {
            model.render(renderContext, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, colour)
        }
        model.setDefault()
    } else {
        renderSprite(matrixStack, sprite, geometry)
    }
    heldItem?.let { HeldItemRenderer().extractOnModel(it, state, matrixStack, geometry, LightCoordsUtil.FULL_BRIGHT) }
    ModelPreviewRenderState.extract(context, geometry.build())
}

const val HEAD_YAW_FACTOR = 40f
const val HEAD_PITCH_FACTOR = 20f
const val SMOOTHING_FACTOR = 0.05f

fun calculateHeadYawAndPitch(
    xCoordinate: Float,
    yCoordinate: Float,
    rotation: Quaternionf,
    mouseX: Int,
    mouseY: Int,
    currentYaw: Float,
    currentPitch: Float,
    resetToCenter: Boolean,
): Pair<Float, Float> {
    val horizontalAngle = atan(((xCoordinate - mouseX) / 40.0f).toDouble()).toFloat()
    val verticalAngle = atan(((yCoordinate - mouseY) / 40.0f).toDouble()).toFloat()

    val eulerXYZRotationDegrees = rotation.toEulerXYZDegrees()
    val targetYaw = if (!resetToCenter) horizontalAngle * HEAD_YAW_FACTOR - eulerXYZRotationDegrees.y else 0f
    val targetPitch = if (!resetToCenter) -verticalAngle * HEAD_PITCH_FACTOR - eulerXYZRotationDegrees.x else 0f

    val newYaw = Mth.rotLerp(SMOOTHING_FACTOR, currentYaw, targetYaw)
    val newPitch = Mth.rotLerp(SMOOTHING_FACTOR, currentPitch, targetPitch)

    return Pair(newYaw, newPitch)
}