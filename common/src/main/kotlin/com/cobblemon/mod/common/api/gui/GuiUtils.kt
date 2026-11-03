/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay.Companion.PORTRAIT_DIAMETER
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.util.toHex
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.LightCoordsUtil
import net.minecraft.client.renderer.rendertype.RenderTypes
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.ModelPreviewRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence

@JvmOverloads
fun drawCenteredText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: Component,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = (text as MutableComponent).let { if (font != null) it.font(font) else it }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt() - textRenderer.width(comp) / 2, y.toInt(), textColour(colour), shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true,
    pMouseX: Number? = null,
    pMouseY: Number? = null
): Boolean {
    val comp = if (font == null) text else text.font(font)
    val textRenderer = Minecraft.getInstance().font
    var x = x
    val width = textRenderer.width(comp)
    if (centered) {
        x = x.toDouble() - width / 2
    }
    context.text(textRenderer, comp, x.toInt(), y.toInt(), textColour(colour), shadow)
    var isHovered = false
    if (pMouseY != null && pMouseX != null) {
        if (pMouseX.toInt() >= x.toInt() && pMouseX.toInt() <= x.toInt() + width &&
            pMouseY.toInt() >= y.toInt() && pMouseY.toInt() <= y.toInt() + textRenderer.lineHeight
        ) {
            isHovered = true
        }
    }
    return isHovered
}

@JvmOverloads
fun drawTextJustifiedRight(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = text.let { if (font != null) it.font(font) else it }
    val font = Minecraft.getInstance().font
    context.text(font, comp, x.toInt() - font.width(comp), y.toInt(), textColour(colour), shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    text: FormattedCharSequence,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true
) {
    val textRenderer = Minecraft.getInstance().font
    var tweakedX = x
    if (centered) {
        val width = textRenderer.width(text)
        tweakedX = tweakedX.toDouble() - width / 2
    }
    context.text(textRenderer, text, tweakedX.toInt(), y.toInt(), textColour(colour), shadow)
}

@JvmOverloads
fun drawString(
    context: GuiGraphicsExtractor,
    text: String,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true,
    font: Identifier? = null
) {
    val comp = Component.literal(text).also {
        font?.run {
            it.font(this)
        }
    }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt(), y.toInt(), textColour(colour), shadow)
}

@JvmOverloads
fun drawPosablePortrait(
    identifier: Identifier,
    context: GuiGraphicsExtractor,
    scale: Float = 13F,
    contextScale: Float = 1F,
    reversed: Boolean = false,
    state: PosableState,
    partialTicks: Float,
    limbSwing: Float = 0F,
    limbSwingAmount: Float = 0F,
    ageInTicks: Float = 0F,
    headYaw: Float = 0F,
    headPitch: Float = 0F,
    doQuirks: Boolean = true,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F
) {
    val matrixStack = PoseStack()
    val geometry = ModelGeometry.Builder()
    matrixStack.pushPose()
    matrixStack.translate(0.0, PORTRAIT_DIAMETER.toDouble() + 2.0, 0.0)
    matrixStack.scale(scale, scale, -scale)
    matrixStack.translate(0.0, -PORTRAIT_DIAMETER / 18.0, 0.0)

    val sprite = VaryingModelRepository.getSprite(identifier, state, SpriteType.PORTRAIT)

    if (sprite == null) {
        val model = VaryingModelRepository.getPoser(identifier, state)
        state.currentModel = model
        val texture = VaryingModelRepository.getTexture(identifier, state)

        val renderContext = RenderContext()
        model.context = renderContext
        VaryingModelRepository.getTextureNoSubstitute(identifier, state).let { renderContext.put(RenderContext.TEXTURE, it) }
        renderContext.put(RenderContext.SCALE, contextScale)
        renderContext.put(RenderContext.SPECIES, identifier)
        renderContext.put(RenderContext.ASPECTS, state.currentAspects)
        renderContext.put(RenderContext.POSABLE_STATE, state)
        renderContext.put(RenderContext.DO_QUIRKS, doQuirks)

        val renderType = RenderTypes.entityCutoutCull(texture)

        val quaternion1 = Axis.YP.rotationDegrees(-32F * if (reversed) -1F else 1F)
        val quaternion2 = Axis.XP.rotationDegrees(5F)

        val originalPose = state.currentPose
        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        state.updatePartialTicks(partialTicks)
        model.applyAnimations(null, state, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
        originalPose?.let { state.setPose(it) }

        matrixStack.translate(
            model.portraitTranslation.x * if (reversed) -1F else 1F,
            model.portraitTranslation.y + 1.5 * model.portraitScale,
            model.portraitTranslation.z - 4
        )
        matrixStack.scale(model.portraitScale, model.portraitScale, 1 / model.portraitScale)
        matrixStack.rotate(quaternion1)
        matrixStack.rotate(quaternion2)

        val buffer = geometry.getBuffer(renderType)
        val packedLight = LightCoordsUtil.pack(11, 7)

        val colour = toHex(r, g, b, a)
        model.withLayerContext(geometry, state, VaryingModelRepository.getLayers(identifier, state)) {
            model.render(renderContext, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, colour)
        }

        model.setDefault()

    } else {
        renderSprite(matrixStack, sprite, geometry)
    }

    matrixStack.popPose()
    ModelPreviewRenderState.extract(context, geometry.build(), ModelPreviewRenderState.LightingMode.PORTRAIT)
}

fun drawProfile(
    resourceIdentifier: Identifier,
    context: GuiGraphicsExtractor,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F
) {
    val matrixStack = PoseStack()
    val geometry = ModelGeometry.Builder()
    matrixStack.scale(scale, scale, -scale)

    val sprite = VaryingModelRepository.getSprite(resourceIdentifier, state, SpriteType.PROFILE)

    if (sprite == null) {

        val model = VaryingModelRepository.getPoser(resourceIdentifier, state)
        val texture = VaryingModelRepository.getTexture(resourceIdentifier, state)

        val renderContext = RenderContext()
        model.context = renderContext
        VaryingModelRepository.getTextureNoSubstitute(resourceIdentifier, state).let { renderContext.put(RenderContext.TEXTURE, it) }
        renderContext.put(RenderContext.SCALE, 1F)
        renderContext.put(RenderContext.SPECIES, resourceIdentifier)
        renderContext.put(RenderContext.ASPECTS, state.currentAspects)
        renderContext.put(RenderContext.POSABLE_STATE, state)
        state.currentModel = model

        val renderType = RenderTypes.entityCutoutCull(texture)//model.getLayer(texture)

        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        state.updatePartialTicks(partialTicks)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)
        matrixStack.translate(
            model.profileTranslation.x,
            model.profileTranslation.y + 1.5 * model.profileScale,
            model.profileTranslation.z - 4.0
        )
        matrixStack.scale(model.profileScale, model.profileScale, 1 / model.profileScale)
//    matrixStack.multiply(rotation)
        val quaternion1 = Axis.YP.rotationDegrees(-32F * if (false) -1F else 1F)
        val quaternion2 = Axis.XP.rotationDegrees(5F)
        matrixStack.rotate(quaternion1)
        matrixStack.rotate(quaternion2)
        val buffer = geometry.getBuffer(renderType)
        val packedLight = LightCoordsUtil.pack(11, 7)

        model.withLayerContext(geometry, state, VaryingModelRepository.getLayers(resourceIdentifier, state)) {
            model.render(renderContext, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }
        model.setDefault()
    } else {
        renderSprite(matrixStack, sprite, geometry)
    }
    ModelPreviewRenderState.extract(context, geometry.build())
}

/** Captures the sprite alternative in the same 3D coordinate system as model previews. */
fun renderSprite(matrixStack: PoseStack, sprite: Identifier, geometry: ModelGeometry.Builder) {
    matrixStack.pushPose()
    matrixStack.translate(-1f, 0f, 0f)
    val matrix = matrixStack.last()
    val buffer = geometry.getBuffer(RenderTypes.entityTranslucentEmissive(sprite))
    for ((x, y, u, v) in arrayOf(
        floatArrayOf(2f, 0f, 1f, 0f), floatArrayOf(0f, 0f, 0f, 0f),
        floatArrayOf(0f, 2f, 0f, 1f), floatArrayOf(2f, 2f, 1f, 1f)
    )) {
        buffer.addVertex(matrix, x, y, 0f).setColor(-1).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal(matrix, 0f, 0f, 1f)
    }
    matrixStack.popPose()
}
