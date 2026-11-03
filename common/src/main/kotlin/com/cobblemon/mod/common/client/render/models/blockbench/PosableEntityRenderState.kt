/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench

import com.cobblemon.mod.common.client.render.ModelGeometry
import net.minecraft.client.renderer.entity.state.EntityRenderState

/** World render state owns geometry, never the entity or its mutable animation delegate. */
class PosableEntityRenderState : EntityRenderState() {
    var geometry: ModelGeometry? = null
}
