/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.placementmodifier

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate
import net.minecraft.world.level.levelgen.placement.PlacementContext
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import java.util.function.Consumer

/**
 * It's like the count placement modifier, but conditional. If condition fails, doesn't alter the stream.
 *
 * @author Hiroku
 * @since September 23rd, 2023
 */
class ConditionalCountPlacementModifier(
    val predicate: BlockPredicate,
    val count: Int
) : PlacementModifier {
    companion object {
        val MODIFIER_CODEC: MapCodec<ConditionalCountPlacementModifier> = RecordCodecBuilder.mapCodec { instance ->
            instance
                .group(
                    BlockPredicate.CODEC.fieldOf("predicate").forGetter { it.predicate },
                    PrimitiveCodec.INT.fieldOf("count").forGetter { it.count }
                )
                .apply(instance, ::ConditionalCountPlacementModifier)
        }
    }

    override fun codec(): MapCodec<ConditionalCountPlacementModifier> = MODIFIER_CODEC

    override fun modify(context: PlacementContext, random: RandomSource, pos: BlockPos, output: Consumer<BlockPos>) {
        if (predicate.test(context.level, pos)) {
            repeat(times = count) { output.accept(pos) }
        } else {
            output.accept(pos)
        }
    }
}