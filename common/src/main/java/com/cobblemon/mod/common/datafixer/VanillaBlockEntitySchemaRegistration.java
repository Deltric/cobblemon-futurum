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
import net.minecraft.util.datafix.schemas.V1458;
import net.minecraft.util.datafix.schemas.V3439;

/** Saved shapes must be registered before native DFU types are constructed. */
public final class VanillaBlockEntitySchemaRegistration {
    private VanillaBlockEntitySchemaRegistration() {}
    public static void registerBlockEntities(Schema schema, Map<String, Supplier<TypeTemplate>> map) {
        // Stable saved IDs only; initializing block/entity registries here would
        // recurse into game bootstrap while the data fixer is being constructed.
        for (String id : new String[]{"healing_machine", "pc", "berry", "pasture", "habitat_block", "poke_snack"})
            schema.registerSimple(map, "cobblemon:" + id);
        for (String id : new String[]{"lectern", "disc_shelf", "tm_machine", "chest", "display_case"})
            schema.register(map, "cobblemon:" + id, () -> V1458.nameableInventory(schema));
        schema.register(map, "cobblemon:campfire_pot", () -> DSL.optionalFields(
                "Items", DSL.list(References.ITEM_STACK.in(schema)),
                "CustomName", References.TEXT_COMPONENT.in(schema), "PotComponent", References.ITEM_STACK.in(schema)));
        for (String id : new String[]{"fossil_multiblock", "fossil_analyzer"})
            schema.register(map, "cobblemon:" + id, () -> fossils(schema));
        schema.register(map, "cobblemon:restoration_tank", () -> DSL.optionalFields(
                "Items", DSL.list(References.ITEM_STACK.in(schema)), "CustomName", References.TEXT_COMPONENT.in(schema),
                "MonitorDisk", References.ITEM_STACK.in(schema), "MultiblockStore",
                DSL.optionalFields("InsertedFossilStacks", DSL.list(References.ITEM_STACK.in(schema)))));
        for (String id : new String[]{"sign", "hanging_sign"})
            schema.register(map, "cobblemon:" + id, () -> V3439.sign(schema));
    }

    private static TypeTemplate fossils(Schema schema) {
        return DSL.optionalFields("MonitorDisk", References.ITEM_STACK.in(schema), "MultiblockStore",
                DSL.optionalFields("InsertedFossilStacks", DSL.list(References.ITEM_STACK.in(schema))));
    }

    public static void registerComponents(Schema schema, Map<String, Supplier<TypeTemplate>> map) {
        // PotComponent's codec maps the whole component to an ItemStack. Bait
        // wraps its optional stack; effect definitions remain opaque remainder.
        map.put("cobblemon:cooking_pot_item", () -> References.ITEM_STACK.in(schema));
        map.put("cobblemon:bait", () -> DSL.optionalFields("stack", References.ITEM_STACK.in(schema)));
    }
}
