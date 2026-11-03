/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.item

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.Compostable
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders
import java.util.IdentityHashMap

/** Collects item registrations until the loader binds their default components. */
object CobblemonComposting {
    private val entries = IdentityHashMap<Item, Compostable>()

    fun register(item: ItemLike, chance: Float) = register(item, providerForChance(chance))

    /** Additional probabilities can supply a datapack-defined native provider. */
    fun register(item: ItemLike, provider: ResourceKey<ContextIntProvider>) {
        entries[item.asItem()] = Compostable(provider)
    }

    fun forEach(action: (Item, Compostable) -> Unit) = entries.forEach(action)

    fun providerForChance(chance: Float): ResourceKey<ContextIntProvider> = when (chance) {
        .30F -> ContextIntProviders.COMPOSTABLE_LOW
        .50F -> ContextIntProviders.COMPOSTABLE_LOW_MEDIUM
        .65F -> ContextIntProviders.COMPOSTABLE_MEDIUM
        .85F -> ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH
        1F -> ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE
        .35F -> customProvider(35)
        .75F -> customProvider(75)
        .80F -> customProvider(80)
        .95F -> customProvider(95)
        else -> throw IllegalArgumentException("No native compost provider for chance $chance; register a ResourceKey<ContextIntProvider> instead")
    }

    private fun customProvider(percent: Int): ResourceKey<ContextIntProvider> =
        ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, Identifier.fromNamespaceAndPath("cobblemon", "compostable/chance_$percent"))
}
