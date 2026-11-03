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
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V705;

/** The exact item templates copied by Cobblemon's original 1.21.1 schemas. */
public final class HistoricalItemStackTemplates extends V705 {
    private static final Set<String> LEGACY_BOATS = Set.of("boat", "oak_boat", "spruce_boat", "birch_boat",
            "jungle_boat", "acacia_boat", "cherry_boat", "dark_oak_boat", "mangrove_boat", "bamboo_raft");
    private static final Set<String> LEGACY_CHEST_BOATS = Set.of("oak_chest_boat", "spruce_chest_boat",
            "birch_chest_boat", "jungle_chest_boat", "acacia_chest_boat", "cherry_chest_boat",
            "dark_oak_chest_boat", "mangrove_chest_boat", "bamboo_chest_raft");
    private static final Hook.HookFunction HISTORICAL_ADD_NAMES = new Hook.HookFunction() {
        @Override
        public <T> T apply(DynamicOps<T> ops, T value) {
            var item = new Dynamic<>(ops, value);
            // Native-first chunks can already contain modern stacks. Legacy
            // lookalike fields on those stacks are opaque, including tag.
            if (item.get("count").result().isPresent() || item.get("components").result().isPresent()) return value;
            return ADD_NAMES.apply(ops, nameLegacyBoatEntity(item).getValue());
        }
    };
    private HistoricalItemStackTemplates() {
        super(0, null);
    }

    // Native schemas can change retrospectively: 26.3 adds text-component
    // recursion to 3818.0 and a hook to 3818.5. Copying those new templates into
    // our old recursive family breaks source-0 migration. Freeze the original
    // templates, including their original recursive indices, instead.
    public static TypeTemplate beforeComponents() {
        return DSL.named("item_stack", DSL.hook(DSL.optionalFields(
                "id", namespaced("item_name"),
                "tag", DSL.optionalFields(
                        "EntityTag", DSL.id(1),
                        "BlockEntityTag", DSL.id(0),
                        "CanDestroy", DSL.list(namespaced("block_name")),
                        "CanPlaceOn", DSL.list(namespaced("block_name")),
                        DSL.optionalFields(
                                "Items", DSL.list(DSL.id(3)),
                                "ChargedProjectiles", DSL.list(DSL.id(3))))),
                HISTORICAL_ADD_NAMES, Hook.HookFunction.IDENTITY));
    }

    public static TypeTemplate withComponents() {
        return DSL.named("item_stack", DSL.optionalFields(
                "id", namespaced("item_name"),
                "components", DSL.id(5)));
    }

    private static TypeTemplate namespaced(String name) {
        return DSL.named(name, DSL.constType(NamespacedSchema.namespacedString()));
    }

    /** Apply original item-name inference along native typed paths, retaining opaque data. */
    public static <T> Dynamic<T> prepareLegacyItem(Dynamic<T> item) {
        return prepare(NativeTemplate.TYPE, item);
    }
    private static <T, A> Dynamic<T> prepare(Type<A> type, Dynamic<T> item) {
        return type.read(item).flatMap(pair -> type.writeDynamic(item.getOps(), pair.getFirst()))
                .result().orElse(item);
    }

    // A private recursive family keeps every native entity and block-entity
    // inventory path. Its original item hook infers generic boat IDs before
    // current native DFU reads them; arbitrary lookalike custom data is opaque.
    // Initialize lazily because building Cobblemon's schemas can initialize DFU.
    private static final class NativeTemplate {
        private static final Type<?> TYPE = new Schema(38180, DataFixers.getDataFixer().getSchema(38180)) {
            @Override
            public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entities,
                                      Map<String, Supplier<TypeTemplate>> blockEntities) {
                super.registerTypes(schema, entities, blockEntities);
                schema.registerType(true, References.ITEM_STACK, () -> DSL.hook(DSL.optionalFields(
                        "id", References.ITEM_NAME.in(schema),
                        "tag", DSL.optionalFields(
                                "EntityTag", References.ENTITY_TREE.in(schema),
                                "BlockEntityTag", References.BLOCK_ENTITY.in(schema),
                                "CanDestroy", DSL.list(References.BLOCK_NAME.in(schema)),
                                "CanPlaceOn", DSL.list(References.BLOCK_NAME.in(schema)),
                                DSL.optionalFields(
                                        "Items", DSL.list(References.ITEM_STACK.in(schema)),
                                        "ChargedProjectiles", DSL.list(References.ITEM_STACK.in(schema))))),
                        HISTORICAL_ADD_NAMES, Hook.HookFunction.IDENTITY));
            }
        }.getType(References.ITEM_STACK);
    }

    private static <T> Dynamic<T> nameLegacyBoatEntity(Dynamic<T> item) {
        String id = NamespacedSchema.ensureNamespaced(item.get("id").asString(""));
        String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : "";
        String entityId = LEGACY_BOATS.contains(path) ? "minecraft:boat"
                : LEGACY_CHEST_BOATS.contains(path) ? "minecraft:chest_boat" : null;
        if (entityId == null) return item;
        return item.update("tag", tag -> tag.update("EntityTag", entity ->
                entity.get("id").result().isPresent() ? entity : entity.set("id", entity.createString(entityId))));
    }

}
