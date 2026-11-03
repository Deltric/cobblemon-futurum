/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import com.cobblemon.mod.common.client.render.ModelGeometry
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import java.util.function.Consumer
import org.joml.Vector3fc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.util.LightCoordsUtil
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f

class PokemonItemRenderer : SpecialModelRenderer<PokemonItemRenderer.Snapshot> {
    data class Snapshot(val geometry: ModelGeometry, val gui: Boolean)

    override fun submit(snapshot: Snapshot?, matrices: PoseStack, collector: SubmitNodeCollector,
                        light: Int, overlay: Int, hasFoil: Boolean, outlineColor: Int) {
        snapshot ?: return
        snapshot.geometry.submitWithLight(matrices, collector,
            if (snapshot.gui) LightCoordsUtil.pack(13, 13) else light, OverlayTexture.NO_OVERLAY, outlineColor)
    }

    // The custom ItemModel supplies per-species extents and the actual display context.
    override fun extractArgument(stack: ItemStack): Snapshot? = extractGeometry(stack, ItemDisplayContext.NONE)
    override fun getExtents(output: Consumer<Vector3fc>) { output.accept(Vector3f()) }

    val context = RenderContext().also {
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
        it.put(RenderContext.DO_QUIRKS, false)
    }

    fun extractGeometry(stack: ItemStack, mode: ItemDisplayContext): Snapshot? {
        val matrices = PoseStack()
        val vertexConsumers = ModelGeometry.Builder()
        val pokemonItem = stack.item as? PokemonItem ?: return null
        val (species, aspects) = pokemonItem.getSpeciesAndAspects(stack) ?: return null
        val state = FloatingState()
        state.currentAspects = aspects
        matrices.pushPose()
        val model = VaryingModelRepository.getPoser(species.resourceIdentifier, state)
        model.context = context
        context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
        context.put(RenderContext.SPECIES, species.resourceIdentifier)
        context.put(RenderContext.ASPECTS, aspects)
        context.put(RenderContext.POSABLE_STATE, state)
        state.currentModel = model

        val transformations = positions[mode]!!

        matrices.scale(transformations.scale.x, transformations.scale.y, transformations.scale.z)
        matrices.translate(transformations.translation.x, transformations.translation.y, transformations.translation.z)
        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)

        matrices.scale(model.profileScale, model.profileScale, 0.15F)

        val rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(transformations.rotation.x, transformations.rotation.y, transformations.rotation.z))
        matrices.rotate(rotation)
        rotation.conjugate()

        val texture = VaryingModelRepository.getTexture(species.resourceIdentifier, state)
        val renderLayer = if (stack.hasFoil()) com.cobblemon.mod.common.client.render.PosableRenderLayers.itemGlint(texture) else RenderTypes.entityCutoutCull(texture)
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)

        matrices.pushPose()
        val packedLight = if (mode == ItemDisplayContext.GUI) {
            LightCoordsUtil.pack(13, 13)
        } else {
            0
        }

        // x = red, y = green, z = blue, w = alpha
        val tint = pokemonItem.tint(stack)
        model.withLayerContext(vertexConsumers, state, VaryingModelRepository.getLayers(species.resourceIdentifier, state)) {
            val tintRed = (tint.x * 255).toInt()
            val tintGreen = (tint.y * 255).toInt()
            val tintBlue = (tint.z * 255).toInt()
            val tintAlpha = (tint.w * 255).toInt()
            val color = (tintAlpha shl 24) or (tintRed shl 16) or (tintGreen shl 8) or tintBlue
            model.render(context, matrices, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color)
        }

        model.setDefault()
        matrices.popPose()
        matrices.popPose()

        return Snapshot(vertexConsumers.build(), mode == ItemDisplayContext.GUI)
    }

    companion object {
        val positions: MutableMap<ItemDisplayContext, Transformations> = mutableMapOf()

        init {
            positions[ItemDisplayContext.GUI] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.2, -0.5),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -3.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.FIXED] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.5, -5.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.1F),
                PokemonItemRenderer().Transformation(0F, 35F - 180F, 0F)
            )
            positions[ItemDisplayContext.FIRST_PERSON_RIGHT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(2.75, 0.0, 1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.FIRST_PERSON_LEFT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(-0.75, 0.0, 1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, -35F, 0F)
            )
            positions[ItemDisplayContext.THIRD_PERSON_RIGHT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -1.0, -1.25),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.THIRD_PERSON_LEFT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -1.0, -1.25),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, -35F, 0F)
            )
            positions[ItemDisplayContext.GROUND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.5, -1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.HEAD] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -3.5, 3.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 215F, 0F)
            )
            positions[ItemDisplayContext.NONE] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(0.0, 0.0, 0.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 0F, 0F)
            )
        }
    }

    inner class Transformations(val translation: Transformation<Double>, val scale: Transformation<Float>, val rotation: Transformation<Float>)
    inner class Transformation<T>(val x: T, val y: T, val z: T)
}