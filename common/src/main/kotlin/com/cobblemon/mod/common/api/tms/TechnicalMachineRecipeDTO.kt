/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.util.adapters.RegistryOpsProvider
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.Ingredient

/**
 * DTO used for [TechnicalMachineRecipe] JSON loading because Gson cannot deserialize Minecraft Ingredients.
 */
data class TechnicalMachineRecipeDTO(
    val item: Identifier?,
    val tag: Identifier?,
    @SerializedName("count") private val _count: Int?,
) {
    private val count: Int
        get() = _count ?: 1

    fun toTechnicalMachineRecipe(): TechnicalMachineRecipe {
        val ingredient = when {
            item != null ->
                Ingredient.of(
                    BuiltInRegistries.ITEM.getValue(item) ?: error("Unknown recipe item: $item")
                )

            tag != null ->
                Ingredient.of(
                    (RegistryOpsProvider.getRegistryLookupWithDefaultFallback()
                        ?.lookupOrThrow(Registries.ITEM) ?: BuiltInRegistries.ITEM)
                        .getOrThrow(TagKey.create(Registries.ITEM, tag))
                )

            else ->
                error("Recipe entry must define either 'item' or 'tag'")
        }

        return TechnicalMachineRecipe(
            ingredient = ingredient,
            count = count
        )
    }
}
