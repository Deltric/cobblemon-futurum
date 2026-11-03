/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.color

import net.minecraft.world.item.ItemStack

/** Shared color calculation also used by block previews, independent of native item model registration. */
fun interface CobblemonItemColorProvider {
    fun getColor(stack: ItemStack, layer: Int): Int
}
