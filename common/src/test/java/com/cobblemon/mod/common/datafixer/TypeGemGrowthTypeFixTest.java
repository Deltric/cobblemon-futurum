/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeGemGrowthTypeFixTest {
    @BeforeAll
    static void initialize() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static CompoundTag chunkWith(String properties) throws Exception {
        return TagParser.parseCompoundFully("{xPos:0,zPos:0,Status:'minecraft:full',sections:[{Y:4b,block_states:{palette:["
                + "{Name:'minecraft:stone'},"
                + "{Name:'cobblemon:fire_gem_cluster',Properties:" + properties + "}]}}]}");
    }

    private static CompoundTag cluster(CompoundTag chunk) {
        return chunk.getListOrEmpty("sections").getCompoundOrEmpty(0).getCompoundOrEmpty("block_states")
                .getListOrEmpty("palette").getCompoundOrEmpty(1).getCompoundOrEmpty("Properties");
    }

    @Test
    void mergesOldPropertiesIntoGrowthType() throws Exception {
        var cases = new String[][]{
                {"{facing:'up',should_grow:'true',stunted:'false'}", "full"},
                {"{facing:'up',should_grow:'false',stunted:'false'}", "none"},
                {"{facing:'up',should_grow:'true',stunted:'true'}", "stunted"},
                {"{facing:'up'}", "full"}
        };
        for (var entry : cases) {
            var fixed = DataFixTypes.CHUNK.update(CobblemonSchemas.getDATA_FIXER(), chunkWith(entry[0]), 2, CobblemonSchemas.DATA_VERSION);
            var properties = cluster(fixed);
            assertEquals(entry[1], properties.getStringOr("growth_type", ""), entry[0]);
            assertFalse(properties.contains("should_grow"), entry[0]);
            assertFalse(properties.contains("stunted"), entry[0]);
            assertEquals("up", properties.getStringOr("facing", ""), entry[0]);
        }
    }

    @Test
    void earlierPortVersionThreeChunksAreStillMigrated() throws Exception {
        // Earlier builds of this port used version 3 for the item upgrade only.
        var fixed = DataFixTypes.CHUNK.update(CobblemonSchemas.getDATA_FIXER(),
                chunkWith("{facing:'north',should_grow:'false',stunted:'true'}"), 3, CobblemonSchemas.DATA_VERSION);
        assertEquals("stunted", cluster(fixed).getStringOr("growth_type", ""));
    }

    @Test
    void leavesMigratedClustersUnchanged() throws Exception {
        var chunk = chunkWith("{facing:'up',growth_type:'none'}");
        assertEquals(chunk, DataFixTypes.CHUNK.update(CobblemonSchemas.getDATA_FIXER(), chunk.copy(), 3, CobblemonSchemas.DATA_VERSION));
    }
}
