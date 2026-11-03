/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonItemComponentsFixTest {
    private static JsonObject cases;

    @BeforeAll
    static void initialize() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try (var input = PokemonItemComponentsFixTest.class.getResourceAsStream("/migration/item-components-1.21.1.json")) {
            assertNotNull(input);
            cases = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject pokemon(String name) {
        var pokemon = new JsonObject();
        pokemon.addProperty("cobblemon:data_version", 2);
        pokemon.add("HeldItem", cases.get(name).deepCopy());
        pokemon.add("CosmeticItem", cases.get(name).deepCopy());
        pokemon.add("unknown", JsonParser.parseString("{\"nested\":[12.375,\"preserve\"],\"attributes\":[{\"id\":\"minecraft:generic.example\",\"base\":7}]}"));
        return pokemon;
    }

    private static JsonObject fix(JsonObject input, int sourceVersion) {
        return CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                new Dynamic<>(JsonOps.INSTANCE, input), sourceVersion, CobblemonSchemas.DATA_VERSION).getValue().getAsJsonObject();
    }

    private static CompoundTag nbt(JsonElement json) {
        return (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
    }

    @Test
    void upgradesHeldAndCosmeticComponentsWithoutLosingBehavior() {
        var fixed = fix(pokemon("combined"), 2);
        assertEquals(fixed.get("HeldItem"), fixed.get("CosmeticItem"));
        var components = fixed.getAsJsonObject("HeldItem").getAsJsonObject("components");
        assertEquals(42, components.getAsJsonObject("minecraft:custom_model_data").getAsJsonArray("floats").get(0).getAsInt());
        assertEquals(16711935, components.get("minecraft:dyed_color").getAsInt());
        assertEquals("Component witness", components.getAsJsonObject("minecraft:custom_name").get("text").getAsString());
        var consumable = components.getAsJsonObject("minecraft:consumable");
        assertEquals(0.7, consumable.get("consume_seconds").getAsDouble(), 0.000001);
        var effect = consumable.getAsJsonArray("on_consume_effects").get(0).getAsJsonObject();
        assertEquals(0.25, effect.get("probability").getAsDouble());
        assertEquals(200, effect.getAsJsonArray("effects").get(0).getAsJsonObject().get("duration").getAsInt());
        var remainder = components.getAsJsonObject("minecraft:use_remainder");
        assertEquals("minecraft:glass_bottle", remainder.get("id").getAsString());
        assertEquals(19, remainder.getAsJsonObject("components").getAsJsonObject("minecraft:custom_model_data").getAsJsonArray("floats").get(0).getAsInt());
        var modifier = components.getAsJsonArray("minecraft:attribute_modifiers").get(0).getAsJsonObject();
        assertEquals("minecraft:attack_damage", modifier.get("type").getAsString());
        assertEquals(3.25, modifier.get("amount").getAsDouble());
        assertEquals(pokemon("combined").get("unknown"), fixed.get("unknown"));
    }

    @Test
    void allOldNativeCasesHaveEquivalentJsonAndNbtMigrations() {
        for (String name : cases.keySet()) {
            var source = pokemon(name);
            var json = fix(source, 2);
            var tag = CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                    new Dynamic<>(NbtOps.INSTANCE, nbt(source)), 2, CobblemonSchemas.DATA_VERSION).getValue();
            // JSON has no numeric tag kinds; native fixes deliberately emit NBT
            // floats/ints rather than JsonOps' compact byte/short conversion.
            // NBT also represents booleans as numeric bytes.
            assertEquals(JsonParser.parseString(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbt(json)).toString()),
                    JsonParser.parseString(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag).toString()), name);
        }
    }

    @Test
    void preservesUnknownComponentsAndOpaquePokemonDataExactly() {
        var source = pokemon("combined");
        var unknown = JsonParser.parseString("{\"minecraft:custom_model_data\":91,\"stack\":{\"id\":\"minecraft:stone\",\"components\":{\"minecraft:custom_model_data\":9}},\"amount\":0.17}");
        source.getAsJsonObject("HeldItem").getAsJsonObject("components").add("example:unknown", unknown);
        var fixed = fix(source, 2);
        assertEquals(unknown, fixed.getAsJsonObject("HeldItem").getAsJsonObject("components").get("example:unknown"));
        assertEquals(source.get("unknown"), fixed.get("unknown"));
        var originalTag = nbt(source);
        var fixedTag = (CompoundTag) CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                new Dynamic<>(NbtOps.INSTANCE, originalTag), 2, CobblemonSchemas.DATA_VERSION).getValue();
        assertEquals(originalTag.get("unknown"), fixedTag.get("unknown"));
        assertEquals(originalTag.getCompoundOrEmpty("HeldItem").getCompoundOrEmpty("components").get("example:unknown"),
                fixedTag.getCompoundOrEmpty("HeldItem").getCompoundOrEmpty("components").get("example:unknown"));
        assertEquals(source.getAsJsonObject("HeldItem").getAsJsonObject("components").get("minecraft:custom_data"),
                fixed.getAsJsonObject("HeldItem").getAsJsonObject("components").get("minecraft:custom_data"));
    }

    @Test
    void entityFixThenCodecAndRepeatedSaveDoNotConvertComponentsTwice() {
        var entity = new CompoundTag();
        entity.putString("id", "cobblemon:pokemon"); entity.put("Pokemon", nbt(pokemon("combined")));
        var entities = new net.minecraft.nbt.ListTag(); entities.add(entity);
        var chunk = new CompoundTag(); chunk.put("Entities", entities);
        var fixedChunk = DataFixTypes.ENTITY_CHUNK.update(CobblemonSchemas.getDATA_FIXER(), chunk, 2, CobblemonSchemas.DATA_VERSION);
        var migrated = fixedChunk.getListOrEmpty("Entities").getCompoundOrEmpty(0).getCompoundOrEmpty("Pokemon");
        assertEquals(CobblemonSchemas.DATA_VERSION, migrated.getIntOr("cobblemon:data_version", -1));
        assertEquals(5023, migrated.getIntOr("cobblemon:item_data_version", -1));
        var codec = CobblemonSchemas.INSTANCE.wrapCodec(CompoundTag.CODEC, CobblemonTypeReferences.getPOKEMON());
        var first = codec.encodeStart(NbtOps.INSTANCE, codec.parse(NbtOps.INSTANCE, migrated).getOrThrow()).getOrThrow();
        var second = codec.encodeStart(NbtOps.INSTANCE, codec.parse(NbtOps.INSTANCE, first).getOrThrow()).getOrThrow();
        assertEquals(migrated, first);
        assertEquals(first, second);
        assertEquals(migrated, CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                new Dynamic<>(NbtOps.INSTANCE, migrated), 2, CobblemonSchemas.DATA_VERSION).getValue());
    }

    @Test
    void respectsExplicitNativeVersionOnModernVersionTwoRecords() {
        var modern = fix(pokemon("combined"), 2);
        modern.addProperty("cobblemon:data_version", 2);
        var result = fix(modern, 2);
        assertEquals(modern.get("HeldItem"), result.get("HeldItem"));
        assertEquals(modern.get("CosmeticItem"), result.get("CosmeticItem"));
        assertEquals(5023, result.get("cobblemon:item_data_version").getAsInt());
    }

    @Test
    void componentlessVersionTwoItemsAndMissingFieldsRemainUnchanged() {
        var source = new JsonObject(); source.addProperty("cobblemon:data_version", 2);
        source.add("HeldItem", JsonParser.parseString("{\"id\":\"cobblemon:miracle_seed\",\"count\":1}"));
        var result = fix(source, 2);
        assertEquals(source.get("HeldItem"), result.get("HeldItem"));
        assertFalse(result.has("CosmeticItem"));
        var empty = fix(new JsonObject(), 2);
        assertFalse(empty.has("HeldItem")); assertFalse(empty.has("CosmeticItem"));
    }

    @Test
    void standalonePokemonJsonCodecStampsBothVersionsForPartyPcAndNpcStores() {
        var codec = CobblemonSchemas.INSTANCE.wrapCodec(com.mojang.serialization.Codec.PASSTHROUGH, CobblemonTypeReferences.getPOKEMON());
        var parsed = codec.parse(JsonOps.INSTANCE, pokemon("combined")).getOrThrow();
        var saved = codec.encodeStart(JsonOps.INSTANCE, parsed).getOrThrow();
        assertEquals(CobblemonSchemas.DATA_VERSION, saved.getAsJsonObject().get("cobblemon:data_version").getAsInt());
        assertEquals(5023, saved.getAsJsonObject().get("cobblemon:item_data_version").getAsInt());
        assertEquals(saved, codec.encodeStart(JsonOps.INSTANCE, codec.parse(JsonOps.INSTANCE, saved).getOrThrow()).getOrThrow());
    }

    @Test
    void sourceZeroItemsCompleteAttributeAndLodestoneFixesBeforeModernConversion() throws Exception {
        var items = new String[]{
                "{id:'minecraft:leather_boots',Count:1b,tag:{CustomModelData:42,AttributeModifiers:[{AttributeName:'minecraft:generic.attack_damage',Name:'kept',UUID:[I;1,2,3,4],Amount:3.25d,Operation:0,Slot:'mainhand'}],fixture:{number:0.375d}}}",
                "{id:'minecraft:compass',Count:1b,tag:{LodestonePos:[I;12,65,-4],LodestoneDimension:'minecraft:overworld',LodestoneTracked:0b,fixture:{number:0.375d}}}",
                "{id:'minecraft:compass',count:1,components:{'minecraft:lodestone_target':{pos:[I;12,65,-4],dimension:'minecraft:overworld',tracked:0b}}}",
                "{id:'minecraft:bundle',Count:1b,tag:{Items:[{id:'minecraft:leather_boots',Count:1b,tag:{CustomModelData:19,AttributeModifiers:[{AttributeName:'minecraft:generic.attack_damage',Name:'nested',UUID:[I;4,3,2,1],Amount:1.75d,Operation:0}]}}],fixture:{number:0.375d}}}"
        };
        for (String text : items) {
            var item = TagParser.parseCompoundFully(text);
            var source = new CompoundTag();
            // Keep unrelated species-feature migration outside this item test.
            source.put("Features", new net.minecraft.nbt.ListTag());
            source.put("HeldItem", item.copy()); source.put("CosmeticItem", item.copy());
            source.put("unknown", TagParser.parseCompoundFully("{amount:0.125d,list:[1,2,3]}"));
            var expected = DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, item), 3818, 5023).getValue();
            var fixed = (CompoundTag) CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                    new Dynamic<>(NbtOps.INSTANCE, source), 0, CobblemonSchemas.DATA_VERSION).getValue();
            assertEquals(expected, fixed.get("HeldItem"), text);
            assertEquals(expected, fixed.get("CosmeticItem"), text);
            assertEquals(source.get("unknown"), fixed.get("unknown"));
            assertEquals(5023, fixed.getIntOr("cobblemon:item_data_version", -1));
            var codec = CobblemonSchemas.INSTANCE.wrapCodec(CompoundTag.CODEC, CobblemonTypeReferences.getPOKEMON());
            assertEquals(fixed, codec.encodeStart(NbtOps.INSTANCE, codec.parse(NbtOps.INSTANCE, fixed).getOrThrow()).getOrThrow());
        }
    }

    @Test
    void sourceZeroChunkRulePreservesAlreadyNativeUpgradedBlockEntityItems() throws Exception {
        var chunk = TagParser.parseCompoundFully("{DataVersion:3700,'cobblemon:data_version':0,xPos:0,zPos:0,Status:'minecraft:full',block_entities:[{id:'cobblemon:chest',x:1,y:70,z:2,unknown:{value:0.125d},Items:[{id:'minecraft:leather_boots',count:7,Slot:3b,components:{'minecraft:custom_model_data':{floats:[42.0f]},'example:opaque':{value:0.375d}}}]}]}");
        var fixed = (CompoundTag) CobblemonSchemas.getDATA_FIXER().update(References.CHUNK,
                new Dynamic<>(NbtOps.INSTANCE, chunk), 0, CobblemonSchemas.DATA_VERSION).getValue();
        assertEquals(chunk, fixed);
    }

    @Test
    void sourceOneAndTwoStillUseTheSameReleasedNativeItemInterval() {
        var source = pokemon("combined");
        source.addProperty("cobblemon:data_version", 1);
        assertEquals(fix(source, 2), fix(source, 1));
    }

    @Test
    void sourceZeroEntityItemsMatchTheOriginalNativeUpgradeAndKeepUnknownData() throws Exception {
        JsonObject entityCases;
        try (var input = getClass().getResourceAsStream("/migration/item-entity-data-1.21.1.json")) {
            assertNotNull(input);
            entityCases = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (var entry : entityCases.entrySet()) {
            var fixture = entry.getValue().getAsJsonObject();
            var source = new JsonObject(); source.add("Features", new com.google.gson.JsonArray());
            source.add("HeldItem", fixture.get("source").deepCopy());
            source.add("CosmeticItem", fixture.get("source").deepCopy());
            source.add("unknown", JsonParser.parseString("{\"stack\":{\"id\":\"minecraft:oak_chest_boat\",\"Count\":9,\"tag\":{\"EntityTag\":{\"Type\":\"spruce\"}}}}"));
            var expected = DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(JsonOps.INSTANCE, fixture.get("canonical_1_21_1")), 3955, 5023).getValue();
            var fixed = fix(source, 0);
            assertEquals(expected, fixed.get("HeldItem"), entry.getKey() + " held");
            assertEquals(expected, fixed.get("CosmeticItem"), entry.getKey() + " cosmetic");
            assertEquals(source.get("unknown"), fixed.get("unknown"), entry.getKey() + " opaque Pokemon data");
            assertEquals(expected.getAsJsonObject().get("count"),
                    fixed.getAsJsonObject("HeldItem").get("count"), entry.getKey() + " count");
            var fixedTag = CobblemonSchemas.getDATA_FIXER().update(CobblemonTypeReferences.getPOKEMON(),
                    new Dynamic<>(NbtOps.INSTANCE, nbt(source)), 0, CobblemonSchemas.DATA_VERSION).getValue();
            // JsonOps chooses narrow numeric tags; native fixes intentionally emit
            // int counts and float model data. Compare NBT payload semantics and
            // separately require the native integer count representation.
            assertEquals(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbt(fixed)),
                    NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, fixedTag), entry.getKey() + " NBT payload");
            if (expected.getAsJsonObject().has("count")) {
                assertInstanceOf(net.minecraft.nbt.IntTag.class,
                        ((net.minecraft.nbt.CompoundTag) fixedTag).getCompoundOrEmpty("HeldItem").get("count"));
            }
        }
    }

    @Test
    void sourceZeroDoesNotRecomponentizeAlreadyNativeItemsOrTheirOpaqueCounts() {
        var modern = fix(pokemon("combined"), 2);
        modern.add("Features", new com.google.gson.JsonArray());
        var unknown = JsonParser.parseString("{\"Count\":7,\"tag\":{\"EntityTag\":{\"Type\":\"spruce\"}},\"id\":\"minecraft:oak_chest_boat\"}");
        modern.getAsJsonObject("HeldItem").getAsJsonObject("components").add("example:opaque", unknown);
        modern.getAsJsonObject("HeldItem").addProperty("Count", 91);
        modern.getAsJsonObject("HeldItem").addProperty("id", "minecraft:oak_chest_boat");
        modern.getAsJsonObject("HeldItem").add("tag", JsonParser.parseString("{\"EntityTag\":{\"Type\":\"spruce\",\"opaque\":0.375}}"));
        modern.getAsJsonObject("CosmeticItem").addProperty("Count", 92);
        modern.getAsJsonObject("CosmeticItem").add("tag", unknown.deepCopy());
        var repeated = fix(modern, 0);
        assertEquals(modern.get("HeldItem"), repeated.get("HeldItem"));
        assertEquals(modern.get("CosmeticItem"), repeated.get("CosmeticItem"));
        assertEquals(unknown, repeated.getAsJsonObject("HeldItem").getAsJsonObject("components").get("example:opaque"));
        assertEquals(5023, repeated.get("cobblemon:item_data_version").getAsInt());
        assertEquals(repeated, fix(repeated, 0));
    }

    @Test
    void upstreamVersionThreeRecordsStillReceiveTheItemUpgrade() {
        // Upstream 1.8.2+ stamps version 3 (its type gem fix) on 1.21.1 records
        // that have no item version, so the item upgrade must still run from 3.
        var source = pokemon("combined");
        source.addProperty("cobblemon:data_version", 3);
        var fixed = fix(source, 3);
        var expected = fix(pokemon("combined"), 2);
        assertEquals(expected.get("HeldItem"), fixed.get("HeldItem"));
        assertEquals(expected.get("CosmeticItem"), fixed.get("CosmeticItem"));
        assertEquals(5023, fixed.get("cobblemon:item_data_version").getAsInt());
    }

    @Test
    void earlierPortVersionThreeRecordsAreNotConvertedTwice() {
        // Earlier builds of this port stamped version 3 after upgrading items.
        var saved = fix(pokemon("combined"), 2);
        saved.addProperty("cobblemon:data_version", 3);
        var result = fix(saved, 3);
        assertEquals(saved.get("HeldItem"), result.get("HeldItem"));
        assertEquals(saved.get("CosmeticItem"), result.get("CosmeticItem"));
        assertEquals(5023, result.get("cobblemon:item_data_version").getAsInt());
    }
}
