/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.cooking

import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.net.messages.client.cooking.CookingRecipesPacket
import net.minecraft.world.item.crafting.RecipeHolder

/** Replaced atomically on login or datapack reload; contains no JEI classes. */
object ClientCookingRecipes {
    var recipes: List<RecipeHolder<*>> = emptyList()
        private set
    val changed = SimpleObservable<Unit>()
    private var pending: MutableList<RecipeHolder<*>>? = null

    fun accept(packet: CookingRecipesPacket) {
        if (packet.first) pending = mutableListOf()
        val batch = pending ?: return
        batch.addAll(packet.recipes)
        if (packet.last) {
            recipes = batch.toList()
            pending = null
            changed.emit(Unit)
        }
    }

    fun clear() {
        pending = null
        recipes = emptyList()
        changed.emit(Unit)
    }
}
