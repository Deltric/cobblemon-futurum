/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.toast

import com.cobblemon.mod.common.api.gui.textColour
import net.minecraft.client.renderer.RenderPipelines
import com.cobblemon.mod.common.net.messages.client.toast.ToastPacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import java.util.*
import kotlin.math.min
import net.minecraft.client.gui.Font
import net.minecraft.util.FormattedCharSequence

@Environment(EnvType.CLIENT)
class CobblemonToast(
    val id: UUID,
    var icons: List<ItemStack>,
    var title: Component,
    var description: Component,
    var frameTexture: Identifier,
    var progress: Float,
    var progressColor: Int,
    var durationMs: Long? = null
) : Toast {

    constructor(packet: ToastPacket) : this(
        packet.uuid,
        packet.icons,
        packet.title,
        packet.description,
        packet.frameTexture,
        packet.progress,
        packet.progressColor,
        packet.durationMs
    )

    private var startTime: Long = -1
    private var lastProgress = 0F
    private var lastTime = 0L
    internal var nextVisibility: Toast.Visibility = Toast.Visibility.SHOW

    fun getLines(font: Font, maxWidth: Int): List<FormattedCharSequence> {
        return font.split(this.description, maxWidth)
    }

    override fun getWantedVisibility(): Toast.Visibility = nextVisibility

    override fun update(manager: ToastManager, fullyVisibleForMs: Long) {
        if (startTime == -1L) startTime = fullyVisibleForMs
        val localDuration = durationMs
        if (localDuration != null && fullyVisibleForMs - startTime >= localDuration) {
            ToastTracker.remove(this)
            nextVisibility = Toast.Visibility.HIDE
        }
        lastProgress = Mth.clampedLerp(lastProgress, progress, (fullyVisibleForMs - lastTime).toFloat() / 100F)
        lastTime = fullyVisibleForMs
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, textRenderer: Font, fullyVisibleForMs: Long) {
        val elapsedTime = fullyVisibleForMs - startTime
        val localDuration = durationMs
        if (localDuration != null && elapsedTime >= localDuration) return

        context.blitSprite(RenderPipelines.GUI_TEXTURED, this.frameTexture, 0, 0, this.width(), this.height())

        val maxWidth = 125

        context.blitSprite(RenderPipelines.GUI_TEXTURED, this.frameTexture, 0, 0, this.width(), this.height() + (getLines(textRenderer, maxWidth).size - 1) * 10)

        context.text(textRenderer, this.title, 30, 7, textColour(this.title.style.color?.value ?: -1), false)
        context.textWithWordWrap(textRenderer, this.description, 30, 18, maxWidth, textColour(this.description.style.color?.value ?: -1))

        val iconIndex = if (localDuration != null && localDuration > 0) {
            val repeatsPerIcon = 2
            val totalIcons = icons.size * repeatsPerIcon
            val timePerIcon = (localDuration / totalIcons).coerceAtLeast(1L)

            val iconIndex = (elapsedTime / timePerIcon)
                .toInt()
                .coerceIn(0, totalIcons - 1)

            iconIndex / repeatsPerIcon
        } else {
            val switchIntervalMs = 500L
            ((elapsedTime / switchIntervalMs) % icons.size).toInt()
        }

        val icon = this.icons[iconIndex]
        context.fakeItem(icon, 8, 8)
        if (this.hasProgressBar()) {
            context.fill(3, 28, 157, 29, -1)
            val f = lastProgress
            context.fill(3, 28, (3F + 154F * f).toInt(), 29, this.progressColor)
        }

    }

    internal fun updateFrom(packet: ToastPacket) {
        val newIcons = this.icons + packet.icons
        this.icons = newIcons
        this.title = packet.title
        this.description = packet.description
        this.frameTexture = packet.frameTexture
        this.progress = packet.progress
        this.progressColor = packet.progressColor
        this.lastProgress = min(this.progress, this.lastProgress)
        this.nextVisibility = when (packet.behaviour) {
            ToastPacket.Behaviour.SHOW_OR_UPDATE -> Toast.Visibility.SHOW
            ToastPacket.Behaviour.HIDE -> Toast.Visibility.HIDE
        }
    }

    private fun hasProgressBar(): Boolean = this.progress in 0F..1F

}