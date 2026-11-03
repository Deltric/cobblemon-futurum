/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.grower

import com.cobblemon.mod.common.api.apricorn.Apricorn
import net.minecraft.world.level.block.grower.TreeGrower
import net.minecraft.util.random.WeightedList

class ApricornTreeGrower(apricorn: Apricorn) : TreeGrower(
    apricorn.serializedName,
    WeightedList.of(apricorn.configuredFeature()),
    WeightedList.of(),
    WeightedList.of(),
    apricorn.configuredFeature()
)