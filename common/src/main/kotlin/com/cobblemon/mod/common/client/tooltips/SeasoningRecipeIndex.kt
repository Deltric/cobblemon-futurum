/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.tooltips

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/** Replaced on login/datapack reload and cleared on logout to avoid cross-server recipes. */
object SeasoningRecipeIndex {
    private var processorsByItem: Map<Identifier, Set<String>> = emptyMap()

    fun replace(index: Map<Identifier, Set<String>>) {
        processorsByItem = index.mapValues { (_, types) -> types.toSet() }
    }

    fun clear() {
        processorsByItem = emptyMap()
    }

    fun usesProcessor(stack: ItemStack, type: String): Boolean =
        type in processorsByItem[BuiltInRegistries.ITEM.getKey(stack.item)].orEmpty()
}
