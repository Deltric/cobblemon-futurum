/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.util.cobblemonResource
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.trading.TradeSet

/** Trade sets are loaded from data/cobblemon/trade_set and villager_trade resources. */
object CobblemonTradeOffers {
    val NURSE_TRADE_SETS: Int2ObjectMap<ResourceKey<TradeSet>> = Int2ObjectOpenHashMap<ResourceKey<TradeSet>>().apply {
        for (level in 1..5) put(level, ResourceKey.create(Registries.TRADE_SET, cobblemonResource("nurse/level_$level")))
    }
}
