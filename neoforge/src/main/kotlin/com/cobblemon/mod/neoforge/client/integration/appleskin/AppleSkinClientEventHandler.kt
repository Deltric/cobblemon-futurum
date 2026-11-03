/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge.client.integration.appleskin

import com.cobblemon.mod.common.client.CobblemonClient
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.common.NeoForge
import squeek.appleskin.api.event.HUDOverlayEvent

object AppleSkinClientEventHandler {
    fun register() {
        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onHudLayerEvent(e: HUDOverlayEvent) {
        if (CobblemonClient.pokedexUsageContext.scanningGuiOpen) {
            e.isCanceled = true
        }
    }
}