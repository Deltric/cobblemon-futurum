/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

/** Declares saved entity shapes before vanilla constructs its data-fixer types. */
public final class VanillaEntitySchemaRegistration {
    private VanillaEntitySchemaRegistration() {}

    public static void register(Schema schema, Map<String, Supplier<TypeTemplate>> entities) {
        // Do not initialize CobblemonEntities here: DFU is constructed before mod
        // registries. These are the stable saved IDs, including the no-save types
        // which can still occur in commands, passengers, or entity components.
        for (String path : new String[] {
                "pokemon", "npc", "empty_pokeball", "boat", "poke_bobber", "generic_bedrock"
        }) {
            schema.registerSimple(entities, "cobblemon:" + path);
        }
        schema.register(entities, "cobblemon:chest_boat",
                () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));
        // Native ENTITY already includes ENTITY_EQUIPMENT and CustomName, and
        // ENTITY_TREE includes Passengers. All other Cobblemon fields remain
        // opaque here and continue through Cobblemon's separate data fixer.
    }
}
