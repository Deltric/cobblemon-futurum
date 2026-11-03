/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.fixes.GameRuleRegistryFix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyGameRuleMigrationTest {
    @Test
    void preservesNonDefaultValuesForAllSevenRules() {
        var oldRules = json("""
            {"battleInvulnerability":"true", "doPokemonSpawning":"false",
             "doPokemonLoot":"false", "mobTargetInBattle":"false",
             "doShinyStarters":"true", "healersHealPC":"true",
             "doPokemonPredation":"false"}
            """);
        var expected = json("""
            {"cobblemon:battle_invulnerability":true, "cobblemon:do_pokemon_spawning":false,
             "cobblemon:do_pokemon_loot":false, "cobblemon:mob_target_in_battle":false,
             "cobblemon:do_shiny_starters":true, "cobblemon:healers_heal_pc":true,
             "cobblemon:do_pokemon_predation":false}
            """);
        assertEquals(expected.getValue(), LegacyGameRuleMigration.migrate(oldRules).getValue());
    }

    @Test
    void leavesMissingRulesAndOtherModsUntouched() {
        var original = json("""
            {"minecraft:keep_inventory":true, "anotherModRule":"123"}
            """);
        assertEquals(original.getValue(), LegacyGameRuleMigration.migrate(original).getValue());
    }

    @Test
    void newValueWinsAndMigrationIsIdempotent() {
        var original = json("""
            {"doPokemonLoot":"true", "cobblemon:do_pokemon_loot":false}
            """);
        var migrated = LegacyGameRuleMigration.migrate(original);
        assertEquals(json("{\"cobblemon:do_pokemon_loot\":false}").getValue(), migrated.getValue());
        assertEquals(migrated.getValue(), LegacyGameRuleMigration.migrate(migrated).getValue());
    }

    @Test
    void retainsLegacyBooleanParsing() {
        var migrated = LegacyGameRuleMigration.migrate(json("""
            {"battleInvulnerability":"TRUE", "doPokemonLoot":"invalid"}
            """));
        assertTrue(migrated.get("cobblemon:battle_invulnerability").asBoolean(false));
        assertFalse(migrated.get("cobblemon:do_pokemon_loot").asBoolean(true));
    }

    @Test
    void producesBooleanNbtForSavedWorlds() {
        var oldRules = new CompoundTag();
        oldRules.putString("doPokemonSpawning", "false");
        var dynamic = new Dynamic<Tag>(NbtOps.INSTANCE, oldRules);
        var migrated = (CompoundTag) LegacyGameRuleMigration.migrate(dynamic).getValue();
        assertFalse(migrated.contains("doPokemonSpawning"));
        assertEquals(Tag.TAG_BYTE, migrated.get("cobblemon:do_pokemon_spawning").getId());
        assertFalse(migrated.getBooleanOr("cobblemon:do_pokemon_spawning", true));
    }

    @Test
    void targetVanillaFixRetainsCustomRulesAtTheInjectionPoint() throws ReflectiveOperationException {
        var conversion = GameRuleRegistryFix.class.getDeclaredMethod("lambda$makeRule$2", Dynamic.class);
        conversion.setAccessible(true);
        var original = json("{\"doMobLoot\":\"true\",\"doPokemonLoot\":\"false\"}");
        var vanillaResult = (Dynamic<?>) conversion.invoke(null, original);
        assertEquals("false", vanillaResult.get("doPokemonLoot").asString("missing"));
        var migrated = LegacyGameRuleMigration.migrate(vanillaResult);
        assertTrue(migrated.get("minecraft:mob_drops").asBoolean(false));
        assertFalse(migrated.get("cobblemon:do_pokemon_loot").asBoolean(true));
        assertTrue(migrated.get("doPokemonLoot").result().isEmpty());
    }

    private static Dynamic<JsonElement> json(String value) {
        return new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(value));
    }
}
