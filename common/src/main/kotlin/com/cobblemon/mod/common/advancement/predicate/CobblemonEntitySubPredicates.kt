/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.advancement.predicate

import com.cobblemon.mod.common.platform.PlatformRegistry
import com.mojang.serialization.Codec
import net.minecraft.advancements.predicates.entity.EntitySubPredicate
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey

object CobblemonEntitySubPredicates : PlatformRegistry<Registry<Codec<out EntitySubPredicate>>, ResourceKey<Registry<Codec<out EntitySubPredicate>>>, Codec<out EntitySubPredicate>>() {

    @JvmStatic
    val POKE_POBBER = this.create("poke_bobber", FishingBobberPredicate.CODEC)

    override val registry: Registry<Codec<out EntitySubPredicate>> = BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE

    override val resourceKey: ResourceKey<Registry<Codec<out EntitySubPredicate>>> = Registries.ENTITY_SUB_PREDICATE_TYPE

}