/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.advancement.criterion

import com.mojang.serialization.Codec
import net.minecraft.core.Holder
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.advancements.triggers.SimpleCriterionTrigger as VanillaSimpleCriterionTrigger
import net.minecraft.server.level.ServerPlayer
import java.util.*

class SimpleCriterionTrigger<T, C : SimpleCriterionCondition<T>>(
    val codec: Codec<C>,
) : VanillaSimpleCriterionTrigger<C>() {
    override fun codec() = codec

    fun trigger(player: ServerPlayer, context: T) {
        return this.trigger(player) {
            it.matches(player, context)
        }
    }
}

abstract class SimpleCriterionCondition<T>(
    val playerCtx: Optional<Holder<LootItemCondition>>
) : VanillaSimpleCriterionTrigger.SimpleInstance {
    override fun player() = playerCtx

    abstract fun matches(player: ServerPlayer, context: T): Boolean
}