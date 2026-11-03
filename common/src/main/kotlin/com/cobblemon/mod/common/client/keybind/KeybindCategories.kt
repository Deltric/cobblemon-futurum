/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.keybind

import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object KeybindCategories {
    val COBBLEMON_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cobblemon", "general"))
    val COBBLEMON_DEBUG_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cobblemon", "debug"))
}
