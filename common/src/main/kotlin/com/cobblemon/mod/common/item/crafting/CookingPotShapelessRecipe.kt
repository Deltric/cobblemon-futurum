/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting

import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import java.util.Optional
import com.cobblemon.mod.common.CobblemonItems
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.ItemStackTemplate
import com.cobblemon.mod.common.util.readList
import com.cobblemon.mod.common.util.writeCollection
import com.cobblemon.mod.common.CobblemonRecipeSerializers
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.util.codec.CodecUtils.createByStringCodec
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.writeIdentifier
import com.cobblemon.mod.common.util.writeString
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level

class CookingPotShapelessRecipe(
    override val groupName: String,
    override val category: CookingPotBookCategory,
    override val resultTemplate: ItemStackTemplate,
    private val inputIngredients: List<Ingredient>,
    override val seasoningTag: TagKey<Item>,
    override val seasoningProcessors: List<SeasoningProcessor>
) : Recipe<CraftingInput>, CookingPotRecipeBase {
    override fun getType(): RecipeType<CookingPotShapelessRecipe> = CobblemonRecipeTypes.COOKING_POT_SHAPELESS
    override fun getSerializer() = CobblemonRecipeSerializers.COOKING_POT_SHAPELESS
    override val ingredients: List<Optional<Ingredient>> get() = inputIngredients.map { Optional.of(it) }
    override fun placementInfo(): PlacementInfo = PlacementInfo.create(inputIngredients)
    override fun display(): List<RecipeDisplay> = listOf(ShapelessCraftingRecipeDisplay(
        inputIngredients.map(Ingredient::display), SlotDisplay.ItemStackSlotDisplay(resultTemplate),
        SlotDisplay.ItemSlotDisplay(CobblemonItems.CAMPFIRE_POT_RED)
    ))

    override fun matches(input: CraftingInput, level: Level): Boolean {
        val remaining = inputIngredients.toMutableList()

        for (item in input.items()) {
            if (item.isEmpty) continue

            val matchIndex = remaining.indexOfFirst { it.test(item) }
            if (matchIndex != -1) {
                remaining.removeAt(matchIndex)
            } else {
                return false
            }
        }

        return remaining.isEmpty()
    }

    object Serializer {
            private val CODEC: MapCodec<CookingPotShapelessRecipe> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter { it.groupName },
                    CookingPotBookCategory.CODEC.fieldOf("category").orElse(CookingPotBookCategory.MISC).forGetter { it.category },
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter { it.resultTemplate },
                    Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter { it.inputIngredients },
                    TagKey.codec(Registries.ITEM).fieldOf("seasoningTag").orElse(CobblemonItemTags.EMPTY).forGetter { recipe -> recipe.seasoningTag },
                    createByStringCodec<SeasoningProcessor>(
                        { SeasoningProcessor.processors[it] },
                        { it.type },
                        { "Unknown seasoning processor: $it" }
                    ).listOf().fieldOf("seasoningProcessors").forGetter { recipe -> recipe.seasoningProcessors }
                ).apply(instance, ::CookingPotShapelessRecipe)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, CookingPotShapelessRecipe> =
                StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buffer: RegistryFriendlyByteBuf): CookingPotShapelessRecipe {
                val group = buffer.readUtf()
                val category = buffer.readEnum(CookingPotBookCategory::class.java)
                val seasoningTag = TagKey.create(Registries.ITEM, buffer.readIdentifier())
                val size = buffer.readVarInt()
                val ingredients = List(size) { Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) }
                val result = ItemStackTemplate.STREAM_CODEC.decode(buffer)
                val seasoningProcessors = buffer.readList {
                    val type = buffer.readString()
                    SeasoningProcessor.processors[type] ?: error("Unknown seasoning processor: $type")
                }
                return CookingPotShapelessRecipe(group, category, result, ingredients, seasoningTag, seasoningProcessors)
            }

            private fun toNetwork(buffer: RegistryFriendlyByteBuf, recipe: CookingPotShapelessRecipe) {
                buffer.writeUtf(recipe.groupName)
                buffer.writeEnum(recipe.category)
                buffer.writeIdentifier(recipe.seasoningTag.location)
                buffer.writeVarInt(recipe.inputIngredients.size)
                recipe.inputIngredients.forEach { ingredient ->
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient)
                }
                ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultTemplate)
                buffer.writeCollection(recipe.seasoningProcessors) { _, it ->
                    buffer.writeString(it.type)
                }
            }

        val INSTANCE = RecipeSerializer(CODEC, STREAM_CODEC)
    }
}
