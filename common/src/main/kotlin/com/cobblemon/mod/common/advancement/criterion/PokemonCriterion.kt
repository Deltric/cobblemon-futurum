/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.advancement.criterion

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.server.level.ServerPlayer
import java.util.Optional

class PokemonCriterion(
    playerCtx: Optional<Holder<LootItemCondition>>,
    val species: String,
    val properties: PokemonProperties
): SimpleCriterionCondition<Pokemon>(playerCtx) {

    companion object {
        val CODEC: Codec<PokemonCriterion> = RecordCodecBuilder.create { it.group(
            LootItemCondition.CODEC.optionalFieldOf("player").forGetter(PokemonCriterion::playerCtx),
            Codec.STRING.optionalFieldOf("species", "any").forGetter(PokemonCriterion::species),
            PokemonProperties.CODEC.optionalFieldOf("properties", PokemonProperties()).forGetter(PokemonCriterion::properties)
        ).apply(it, ::PokemonCriterion) }
    }

    override fun matches(player: ServerPlayer, context: Pokemon): Boolean {
        var speciesCheck = (species.equals(context.species.resourceIdentifier.path) || species.equals(context.species.resourceIdentifier.toString())) || species.equals("any")
        return speciesCheck && properties.matches(context)
    }
}
