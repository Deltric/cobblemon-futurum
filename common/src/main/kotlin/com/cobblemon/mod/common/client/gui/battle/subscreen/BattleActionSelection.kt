/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.battle.subscreen

import net.minecraft.client.input.MouseButtonEvent
import com.cobblemon.mod.common.api.gui.ParentWidget
import com.cobblemon.mod.common.client.battle.SingleActionRequest
import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.MutableComponent

abstract class BattleActionSelection(
    val battleGUI: BattleGUI,
    val request: SingleActionRequest,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    name: MutableComponent
) : ParentWidget(x, y, width, height, name) {
    val opacity: Float
        get() = battleGUI.opacity

    // Screens now select a child by isMouseOver before dispatching its click.
    // Battle controls can extend beyond this widget's layout anchor rectangle.
    final override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        isActive && isActionHovered(mouseX, mouseY)

    protected abstract fun isActionHovered(mouseX: Double, mouseY: Double): Boolean

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val pMouseX = event.x()
        val pMouseY = event.y()
        val pButton = event.button()
        if(pButton == InputConstants.MOUSE_BUTTON_LEFT){
            return mousePrimaryClicked(pMouseX, pMouseY)
        }
        return super.mouseClicked(event, doubleClick)
    }

    abstract fun mousePrimaryClicked(pMouseX: Double, pMouseY: Double): Boolean
}
