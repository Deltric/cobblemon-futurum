/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.client.CobblemonClient
import net.minecraft.client.resources.model.geometry.QuadCollection
import net.minecraft.resources.Identifier

/**
 * Contains information for forcing a model to be baked
 *
 * @param modelLocation The location of the model
 * @param modelIdentifier The stable identifier used for the extra-model registration
 */
data class BakingOverride(
    val modelLocation: Identifier,
    val modelIdentifier: Identifier
) {
    fun getModel(): QuadCollection {
        return CobblemonClient.implementation.getBakingOverride(this)
    }
}
