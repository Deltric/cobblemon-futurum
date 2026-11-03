/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.net.messages.client.cooking;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookingRecipesPacketTest {
    private static final Identifier COOKING = Identifier.parse("cobblemon:cooking_pot");
    private static final Identifier SHAPELESS = Identifier.parse("cobblemon:cooking_pot_shapeless");
    private static final Identifier BREWING = Identifier.parse("cobblemon:brewing_stand");
    private static final Identifier ADDON = Identifier.parse("test:addon_cooking_recipe");
    private static final Map<Identifier, RecipeSerializer<SampleRecipe>> SERIALIZERS = Map.of(
        COOKING, serializer(COOKING, 17), SHAPELESS, serializer(SHAPELESS, 29),
        BREWING, serializer(BREWING, 43), ADDON, serializer(ADDON, 61)
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static RecipeSerializer<SampleRecipe> serializer(Identifier id, int marker) {
        return new RecipeSerializer<>(MapCodec.unit(() -> new SampleRecipe(id, "", 0)), StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeByte(marker);
                buffer.writeUtf(recipe.text());
                buffer.writeInt(recipe.amount());
            },
            buffer -> {
                if (buffer.readUnsignedByte() != marker) throw new DecoderException("Wrong recipe serializer");
                return new SampleRecipe(id, buffer.readUtf(), buffer.readInt());
            }
        ));
    }

    private static RegistryAccess registries(Identifier... ids) {
        var registry = new MappedRegistry<RecipeSerializer<?>>(Registries.RECIPE_SERIALIZER, Lifecycle.stable());
        for (var id : ids) Registry.register(registry, id, SERIALIZERS.get(id));
        registry.freeze();
        return new RegistryAccess.ImmutableRegistryAccess(List.of(registry));
    }

    private static RegistryFriendlyByteBuf buffer(RegistryAccess registries) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    }

    private static RecipeHolder<?> holder(Identifier serializer, int number) {
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("test:recipe_" + number)),
            new SampleRecipe(serializer, "complete recipe payload " + number, number));
    }

    @Test
    void reorderedSerializerRegistriesBreakNativeOrdinalCodecButPreserveEntirePacket() {
        var server = registries(COOKING, SHAPELESS, BREWING, ADDON);
        var client = registries(ADDON, SHAPELESS, COOKING, BREWING);
        assertNotEquals(server.lookupOrThrow(Registries.RECIPE_SERIALIZER).getId(SERIALIZERS.get(BREWING)),
            client.lookupOrThrow(Registries.RECIPE_SERIALIZER).getId(SERIALIZERS.get(BREWING)));
        var legacy = buffer(server);
        try {
            RecipeHolder.STREAM_CODEC.encode(legacy, holder(BREWING, 7));
            var reader = new RegistryFriendlyByteBuf(legacy, client);
            assertThrows(DecoderException.class, () -> RecipeHolder.STREAM_CODEC.decode(reader));
        } finally {
            legacy.release();
        }

        var recipes = List.of(holder(BREWING, 7), holder(COOKING, 19), holder(SHAPELESS, 31), holder(ADDON, 97));
        var packet = new CookingRecipesPacket(true, true, recipes);
        var encoded = buffer(server);
        try {
            packet.encode(encoded);
            var decoded = CookingRecipesPacket.Companion.decode(new RegistryFriendlyByteBuf(encoded, client));
            assertTrue(decoded.getFirst());
            assertTrue(decoded.getLast());
            assertEquals(recipes.size(), decoded.getRecipes().size());
            for (int i = 0; i < recipes.size(); i++) {
                assertEquals(recipes.get(i).id(), decoded.getRecipes().get(i).id());
                assertEquals(recipes.get(i).value(), decoded.getRecipes().get(i).value());
            }
            assertFalse(encoded.isReadable(), "Every serialized payload byte must be consumed");
        } finally {
            encoded.release();
        }
    }

    @Test
    void recipeAndSerializerAreBothWrittenAsIdentifiers() {
        var encoded = buffer(registries(COOKING, SHAPELESS, BREWING));
        try {
            CookingRecipeHolderCodec.INSTANCE.encode(encoded, holder(BREWING, 7));
            assertEquals(Identifier.parse("test:recipe_7"), encoded.readIdentifier());
            assertEquals(BREWING, encoded.readIdentifier());
            assertEquals(43, encoded.readUnsignedByte());
            assertEquals("complete recipe payload 7", encoded.readUtf());
            assertEquals(7, encoded.readInt());
            assertFalse(encoded.isReadable());
        } finally {
            encoded.release();
        }
    }

    @Test
    void unknownSerializersFailBeforeReadingThePayload() {
        for (var id : List.of(BREWING, ADDON)) {
            var encoded = buffer(registries(COOKING));
            try {
                encoded.writeIdentifier(Identifier.parse("test:invalid"));
                encoded.writeIdentifier(id);
                int payloadStart = encoded.writerIndex();
                encoded.writeInt(0x12345678);
                assertThrows(DecoderException.class, () -> CookingRecipeHolderCodec.INSTANCE.decode(encoded));
                assertEquals(payloadStart, encoded.readerIndex());
            } finally {
                encoded.release();
            }
        }
        for (var id : List.of(BREWING, ADDON)) {
            var encoded = buffer(registries(COOKING));
            try {
                assertThrows(EncoderException.class, () -> CookingRecipeHolderCodec.INSTANCE.encode(encoded, holder(id, 9)));
                assertEquals(0, encoded.writerIndex());
            } finally {
                encoded.release();
            }
        }
    }

    @Test
    void chunkBoundsAndEmptyOrFullChunksRemainStrict() {
        var registries = registries(COOKING, SHAPELESS, BREWING);
        for (int size : new int[] {0, 64}) {
            var recipes = new ArrayList<RecipeHolder<?>>();
            for (int i = 0; i < size; i++) recipes.add(holder(BREWING, i));
            var encoded = buffer(registries);
            try {
                new CookingRecipesPacket(true, false, recipes).encode(encoded);
                var decoded = CookingRecipesPacket.Companion.decode(encoded);
                assertTrue(decoded.getFirst());
                assertFalse(decoded.getLast());
                assertEquals(size, decoded.getRecipes().size());
                assertFalse(encoded.isReadable());
            } finally {
                encoded.release();
            }
        }
        for (int size : new int[] {-1, 65}) {
            var encoded = buffer(registries);
            try {
                encoded.writeBoolean(false);
                encoded.writeBoolean(true);
                encoded.writeVarInt(size);
                assertThrows(IllegalArgumentException.class, () -> CookingRecipesPacket.Companion.decode(encoded));
            } finally {
                encoded.release();
            }
        }
        var oversized = new ArrayList<RecipeHolder<?>>();
        for (int i = 0; i < 65; i++) oversized.add(holder(BREWING, i));
        var encoded = buffer(registries);
        try {
            assertThrows(IllegalArgumentException.class, () -> new CookingRecipesPacket(true, true, oversized).encode(encoded));
            assertEquals(0, encoded.writerIndex());
        } finally {
            encoded.release();
        }
    }

    private record SampleRecipe(Identifier serializerId, String text, int amount) implements Recipe<RecipeInput> {
        @Override public boolean matches(RecipeInput input, Level level) { return false; }
        @Override public ItemStack assemble(RecipeInput input) { return ItemStack.EMPTY; }
        @Override public boolean showNotification() { return false; }
        @Override public String group() { return ""; }
        @Override public RecipeSerializer<SampleRecipe> getSerializer() { return SERIALIZERS.get(serializerId); }
        @Override public RecipeType<SampleRecipe> getType() { return null; }
        @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
        @Override public RecipeBookCategory recipeBookCategory() { return null; }
    }
}
