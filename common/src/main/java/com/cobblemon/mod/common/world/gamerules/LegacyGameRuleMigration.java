/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules;

import com.mojang.serialization.Dynamic;

/** Converts Cobblemon's pre-registry saved rules alongside the vanilla level data fix. */
public final class LegacyGameRuleMigration {
    private LegacyGameRuleMigration() {}

    public static <T> Dynamic<T> migrate(Dynamic<T> rules) {
        rules = renameBoolean(rules, "battleInvulnerability", "cobblemon:battle_invulnerability");
        rules = renameBoolean(rules, "doPokemonSpawning", "cobblemon:do_pokemon_spawning");
        rules = renameBoolean(rules, "doPokemonLoot", "cobblemon:do_pokemon_loot");
        rules = renameBoolean(rules, "mobTargetInBattle", "cobblemon:mob_target_in_battle");
        rules = renameBoolean(rules, "doShinyStarters", "cobblemon:do_shiny_starters");
        rules = renameBoolean(rules, "healersHealPC", "cobblemon:healers_heal_pc");
        return renameBoolean(rules, "doPokemonPredation", "cobblemon:do_pokemon_predation");
    }

    private static <T> Dynamic<T> renameBoolean(Dynamic<T> rules, String oldName, String newName) {
        var legacyValue = rules.get(oldName).result();
        if (legacyValue.isEmpty()) {
            return rules;
        }
        // A value already saved under the new name takes precedence.
        if (rules.get(newName).result().isEmpty()) {
            boolean value = Boolean.parseBoolean(legacyValue.get().asString(""));
            rules = rules.set(newName, rules.createBoolean(value));
        }
        return rules.remove(oldName);
    }
}
