/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.experience.ExperienceGroup;
import com.cobblemon.mod.common.api.pokemon.experience.ExperienceGroups;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.config.CobblemonConfig;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonPropertiesArgumentTypeTest {
    private final Map<Identifier, Species> previousSpecies = new HashMap<>();
    private CobblemonConfig previousConfig;
    private ExperienceGroup experience;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void saveRegistry() {
        for (var species : PokemonSpecies.getSpecies()) previousSpecies.put(species.getResourceIdentifier(), species);
        PokemonSpecies.INSTANCE.reload(Map.of());
        previousConfig = Cobblemon.config;
        Cobblemon.config = new CobblemonConfig();
        experience = ExperienceGroup.Companion.dummy("command_test");
        ExperienceGroups.INSTANCE.register(experience);
    }

    @AfterEach
    void restoreRegistry() {
        PokemonSpecies.INSTANCE.reload(previousSpecies);
        Cobblemon.config = previousConfig;
        ExperienceGroups.INSTANCE.unregister(experience);
    }

    private static CommandDispatcher<Object> dispatcher(ArrayList<PokemonProperties> received) {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("probe")
                .then(RequiredArgumentBuilder.<Object, PokemonProperties>argument("properties", new PokemonPropertiesArgumentType())
                        .executes(context -> {
                            received.add(PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "properties"));
                            return 1;
                        })));
        return dispatcher;
    }

    @Test
    void cachedFunctionParseResolvesSpeciesAfterRegistryLoadAndEachSubsequentChange() throws Exception {
        var received = new ArrayList<PokemonProperties>();
        var dispatcher = dispatcher(received);
        String raw = "magikarp  level=5 nickname=BattleFin shiny=true moves=splash scale_modifier=1.25";
        var cached = dispatcher.parse("probe " + raw, new Object());
        assertTrue(cached.getExceptions().isEmpty());
        var compiled = cached.getContext().build("probe " + raw).getArgument("properties", PokemonProperties.class);
        assertNull(compiled.getSpecies(), "Function compiled before the species resource was loaded");
        assertEquals(raw, compiled.getOriginalString());

        var id = Identifier.parse("cobblemon:magikarp");
        PokemonSpecies.INSTANCE.reload(Map.of(id, new Species()));
        assertEquals(1, dispatcher.execute(cached));
        var first = received.getFirst();
        assertEquals("magikarp", first.getSpecies());
        assertEquals(raw, first.getOriginalString());
        assertEquals(5, first.getLevel());
        assertEquals("BattleFin", first.getNickname().getString());
        assertEquals(Boolean.TRUE, first.getShiny());
        assertEquals(java.util.List.of("splash"), first.getMoves());
        assertEquals(1.25F, first.getScaleModifier());

        // Command execution may mutate properties. Reusing the compiled
        // function must neither cache that mutation nor retain removed data.
        first.setSpecies("mutated");
        first.setLevel(99);
        assertEquals(1, dispatcher.execute(cached));
        assertEquals("magikarp", received.getLast().getSpecies());
        assertEquals(5, received.getLast().getLevel());
        assertNotSame(first, received.getLast());
        PokemonSpecies.INSTANCE.reload(Map.of());
        assertEquals(1, dispatcher.execute(cached));
        assertNull(received.getLast().getSpecies(), "Spawn command must retain its missing-species validation");
        assertNull(compiled.getSpecies(), "Execution must not rewrite the cached Brigadier argument");
    }

    @Test
    void randomSpeciesRemainsDeferredAndUnknownSpeciesKeepsExistingValidation() throws Exception {
        var received = new ArrayList<PokemonProperties>();
        var dispatcher = dispatcher(received);
        String raw = "random level=5 nickname=RandomFin";
        var cached = dispatcher.parse("probe " + raw, new Object());
        dispatcher.execute(cached);
        dispatcher.execute(cached);
        assertEquals("random", received.getFirst().getSpecies());
        assertEquals("random", received.getLast().getSpecies());
        assertEquals(raw, received.getLast().getOriginalString());
        assertNotSame(received.getFirst(), received.getLast());
        dispatcher.execute("probe unknown_species level=5", new Object());
        assertNull(received.getLast().getSpecies());
    }
}
