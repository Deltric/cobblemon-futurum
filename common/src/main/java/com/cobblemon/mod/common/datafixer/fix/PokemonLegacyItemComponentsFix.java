/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer.fix;

import com.cobblemon.mod.common.datafixer.CobblemonTypeReferences;
import com.cobblemon.mod.common.datafixer.HistoricalItemStackTemplates;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/** Completes the source-0 item transition using full native recursive schemas. */
public final class PokemonLegacyItemComponentsFix extends DataFix {
    public PokemonLegacyItemComponentsFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        // A parent Pokemon rewrite alone runs after recursive item rewrites.
        // First componentize each complete raw typed stack with native schemas,
        // whose entity/block-entity choices include its nested inventories.
        var historicalSchema = getOutputSchema().getParent();
        var inputItemType = historicalSchema.getType(References.ITEM_STACK);
        var outputItemType = getOutputSchema().getType(References.ITEM_STACK);
        var pokemonType = getOutputSchema().getType(CobblemonTypeReferences.getPOKEMON());
        return TypeRewriteRule.seq(
                writeFixAndRead("Complete legacy item componentization", inputItemType, outputItemType, item ->
                        isComponentized(item) ? item : componentize(item)),
                writeFixAndRead("Legacy Pokemon item components through 1.21.1",
                        pokemonType, pokemonType, this::fixPokemon));
    }

    private Dynamic<?> fixPokemon(Dynamic<?> pokemon) {
        int version = pokemon.get(PokemonItemComponentsFix.ITEM_VERSION_KEY).asInt(-1);
        // HeldItem is now fully componentized by the preceding item rule;
        // CosmeticItem was opaque in the historical schema and starts raw.
        // Finish the released interval before schema 3 handles newer formats.
        Dynamic<?> fixed = pokemon
                .update("HeldItem", item -> upgrade(item, version < 0 ? 3819 : version))
                .update("CosmeticItem", item -> upgrade(item,
                        version >= 0 ? version : isComponentized(item) ? 3819 : 3818));
        return fixed.set(PokemonItemComponentsFix.ITEM_VERSION_KEY,
                fixed.createInt(Math.max(version, PokemonItemComponentsFix.LEGACY_ITEM_VERSION)));
    }

    private static <T> Dynamic<T> upgrade(Dynamic<T> item, int fromVersion) {
        return DataFixers.getDataFixer().update(References.ITEM_STACK,
                fromVersion < 3819 ? HistoricalItemStackTemplates.prepareLegacyItem(item) : item,
                fromVersion, PokemonItemComponentsFix.LEGACY_ITEM_VERSION);
    }

    private static boolean isComponentized(Dynamic<?> item) {
        return item.get("count").result().isPresent() || item.get("components").result().isPresent();
    }

    private static <T> Dynamic<T> componentize(Dynamic<T> item) {
        return DataFixers.getDataFixer().update(References.ITEM_STACK,
                HistoricalItemStackTemplates.prepareLegacyItem(item), 3818, 3819);
    }
}
