/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer.fix;

import com.cobblemon.mod.common.datafixer.CobblemonTypeReferences;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/** Migrates the two saved Pokémon item stacks without decoding mod components. */
public final class PokemonItemComponentsFix extends DataFix {
    public static final int FIX_VERSION = 3;
    public static final int LEGACY_ITEM_VERSION = 3955;
    public static final int TARGET_ITEM_VERSION = 5023;
    public static final String ITEM_VERSION_KEY = "cobblemon:item_data_version";
    private static final String POKEMON_VERSION_KEY = "cobblemon:data_version";

    public PokemonItemComponentsFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        // HeldItem is a typed field in the historical Pokémon schema, while
        // CosmeticItem is remainder data. Serialize the entire Pokémon so both
        // pass through the same native upgrade, then retain the schema shape.
        return writeFixAndRead("Pokemon item components for Minecraft 26.3",
                getInputSchema().getType(CobblemonTypeReferences.getPOKEMON()),
                getOutputSchema().getType(CobblemonTypeReferences.getPOKEMON()),
                this::fixPokemon);
    }

    private Dynamic<?> fixPokemon(Dynamic<?> pokemon) {
        // An entity-chunk fix can run before Pokemon.CODEC reads this compound.
        // Native component fixes are not idempotent, so stamp the nested value.
        if (pokemon.get(POKEMON_VERSION_KEY).asInt(0) >= FIX_VERSION) return pokemon;
        int itemVersion = pokemon.get(ITEM_VERSION_KEY).asInt(LEGACY_ITEM_VERSION);
        Dynamic<?> fixed = pokemon
                .update("HeldItem", item -> upgradeItem(item, itemVersion))
                .update("CosmeticItem", item -> upgradeItem(item, itemVersion));
        return fixed.set(POKEMON_VERSION_KEY, fixed.createInt(FIX_VERSION))
                .set(ITEM_VERSION_KEY, fixed.createInt(Math.max(itemVersion, TARGET_ITEM_VERSION)));
    }

    private static <T> Dynamic<T> upgradeItem(Dynamic<T> item, int fromVersion) {
        return DataFixers.getDataFixer().update(References.ITEM_STACK, item, fromVersion, TARGET_ITEM_VERSION);
    }
}
