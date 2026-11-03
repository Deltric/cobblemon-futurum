/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory

object CobblemonGameRules {

    @JvmField
    val BATTLE_INVULNERABILITY: GameRule<Boolean> = register("battle_invulnerability", GameRuleCategory.PLAYER, false)
    @JvmField
    val DO_POKEMON_SPAWNING: GameRule<Boolean> = register("do_pokemon_spawning", GameRuleCategory.SPAWNING, true)
    @JvmField
    val DO_POKEMON_LOOT: GameRule<Boolean> = register("do_pokemon_loot", GameRuleCategory.DROPS, true)
    @JvmField
    val MOB_TARGET_IN_BATTLE: GameRule<Boolean> = register("mob_target_in_battle", GameRuleCategory.MOBS, true)
    @JvmField
    val SHINY_STARTERS: GameRule<Boolean> = register("do_shiny_starters", GameRuleCategory.MISC, false)
    @JvmField
    val HEALERS_HEAL_PC: GameRule<Boolean> = register("healers_heal_pc", GameRuleCategory.MISC, false)

    private fun register(name: String, category: GameRuleCategory, defaultValue: Boolean): GameRule<Boolean> =
        Cobblemon.implementation.registerGameRule(cobblemonResource(name), category, defaultValue)

}
