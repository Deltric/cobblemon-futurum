/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.cooking;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Recipe serializers are not a synchronized Fabric registry in 26.3. */
public final class CookingRecipeHolderCodec implements StreamCodec<RegistryFriendlyByteBuf, RecipeHolder<?>> {
    public static final CookingRecipeHolderCodec INSTANCE = new CookingRecipeHolderCodec();
    private CookingRecipeHolderCodec() {}

    @Override
    public RecipeHolder<?> decode(RegistryFriendlyByteBuf buffer) {
        var recipeId = ResourceKey.create(Registries.RECIPE, buffer.readIdentifier());
        var serializerId = buffer.readIdentifier();
        var serializer = buffer.registryAccess().lookupOrThrow(Registries.RECIPE_SERIALIZER).getValue(serializerId);
        if (serializer == null) {
            throw new DecoderException("Unknown cooking recipe serializer: " + serializerId);
        }
        return new RecipeHolder<>(recipeId, serializer.streamCodec().decode(buffer));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, RecipeHolder<?> holder) {
        encodeRecipe(buffer, holder.id(), holder.value());
    }

    private static <T extends Recipe<?>> void encodeRecipe(
        RegistryFriendlyByteBuf buffer, ResourceKey<Recipe<?>> recipeId, T recipe
    ) {
        // A Recipe's serializer accepts that same recipe type; the native interface
        // expresses it through an input-type wildcard rather than this value's T.
        @SuppressWarnings("unchecked")
        var serializer = (RecipeSerializer<T>) recipe.getSerializer();
        var serializerId = buffer.registryAccess().lookupOrThrow(Registries.RECIPE_SERIALIZER).getKey(serializer);
        if (serializerId == null) {
            throw new EncoderException("Unregistered cooking recipe serializer: " + serializer);
        }
        buffer.writeIdentifier(recipeId.identifier());
        buffer.writeIdentifier(serializerId);
        serializer.streamCodec().encode(buffer, recipe);
    }
}
