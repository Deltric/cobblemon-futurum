/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

fun Inventory.removeAmountIf(amount: Int, rule: Predicate<ItemStack>) {
    (this as Container).removeItemsMatching(amount, rule)
}

/** Remove at most [amount] items, retaining native global slot order across equipment. */
internal fun Container.removeItemsMatching(amount: Int, rule: Predicate<ItemStack>) {
    var remaining = amount
    for (slot in 0 until containerSize) {
        if (remaining <= 0) return
        if (rule.test(getItem(slot))) {
            remaining -= removeItem(slot, remaining).count
        }
    }
}
