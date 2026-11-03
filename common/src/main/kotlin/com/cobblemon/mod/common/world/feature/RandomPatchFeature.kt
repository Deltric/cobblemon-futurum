/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.stream.Stream

/** Preserves the patch placement used by our crops after vanilla removed random_patch. */
class RandomPatchFeature(
    val tries: Int,
    val xzSpread: Int,
    val ySpread: Int,
    val feature: Holder<PlacedFeature>
) : Feature {
    override fun codec(): MapCodec<RandomPatchFeature> = CODEC
    override fun getSubFeatures(): Stream<Holder<Feature>> = feature.value().features

    override fun place(level: WorldGenLevel, chunkGenerator: ChunkGenerator, random: RandomSource, origin: BlockPos): Boolean {
        val position = BlockPos.MutableBlockPos()
        val horizontalBound = xzSpread + 1
        val verticalBound = ySpread + 1
        var placed = false
        repeat(tries) {
            position.setWithOffset(origin,
                random.nextInt(horizontalBound) - random.nextInt(horizontalBound),
                random.nextInt(verticalBound) - random.nextInt(verticalBound),
                random.nextInt(horizontalBound) - random.nextInt(horizontalBound))
            if (feature.value().place(level, chunkGenerator, random, position)) placed = true
        }
        return placed
    }

    companion object {
        val CODEC: MapCodec<RandomPatchFeature> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("tries").orElse(128).forGetter { it.tries },
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("xz_spread").orElse(7).forGetter { it.xzSpread },
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("y_spread").orElse(3).forGetter { it.ySpread },
                PlacedFeature.CODEC.fieldOf("feature").forGetter { it.feature }
            ).apply(instance, ::RandomPatchFeature)
        }
    }
}
