/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature.structure

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType

class CobblemonFeatureConfiguration(
    val cobblemonStructures: List<Identifier>,
    val cobblemonProcessors: Holder<StructureProcessorList>,
) {
    init {
        if (cobblemonStructures.isEmpty()) {
            throw IllegalArgumentException("Cobblemon structure lists need at least one entry")
        }
    }

    companion object {
        val CODEC: MapCodec<CobblemonFeatureConfiguration> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Identifier.CODEC.listOf().fieldOf("cobblemon_structures").forGetter { it.cobblemonStructures },
                StructureProcessorType.LIST_CODEC.fieldOf("cobblemon_processors").forGetter { it.cobblemonProcessors },
            ).apply(instance, ::CobblemonFeatureConfiguration)
        }
    }
}