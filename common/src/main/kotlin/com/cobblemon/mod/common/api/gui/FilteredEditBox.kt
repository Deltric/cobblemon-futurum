/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.util.StringUtil

/** Retains whole-value validation for numeric fields, including paste and selection edits. */
class FilteredEditBox(font: Font, x: Int, y: Int, width: Int, height: Int, message: Component) :
    EditBox(font, x, y, width, height, message) {
    private var filter: (String) -> Boolean = { true }
    private var selectionEnd = 0
    private var maximumLength = 32

    fun setFilter(filter: (String) -> Boolean) {
        this.filter = filter
    }

    override fun setMaxLength(length: Int) {
        maximumLength = length
        super.setMaxLength(length)
    }

    override fun setHighlightPos(position: Int) {
        selectionEnd = position.coerceIn(0, value.length)
        super.setHighlightPos(position)
    }

    override fun setValue(value: String) {
        if (filter(value)) super.setValue(value)
    }

    override fun insertText(input: String) {
        val start = minOf(cursorPosition, selectionEnd)
        val end = maxOf(cursorPosition, selectionEnd)
        var available = maximumLength - value.length + end - start
        if (available <= 0) return
        var text = StringUtil.filterText(input)
        if (text.length > available) {
            if (Character.isHighSurrogate(text[available - 1])) available--
            text = text.substring(0, available)
        }
        if (filter(value.replaceRange(start, end, text))) super.insertText(input)
    }

    override fun deleteCharsToPos(position: Int) {
        if (selectionEnd != cursorPosition) {
            insertText("")
            return
        }
        val start = minOf(position, cursorPosition)
        val end = maxOf(position, cursorPosition)
        if (filter(value.removeRange(start, end))) super.deleteCharsToPos(position)
    }
}
