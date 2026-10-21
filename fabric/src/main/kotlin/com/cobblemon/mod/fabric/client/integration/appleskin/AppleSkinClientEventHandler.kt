/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.client.integration.appleskin

import com.cobblemon.mod.common.client.CobblemonClient
import squeek.appleskin.api.AppleSkinApi
import squeek.appleskin.api.event.HUDOverlayEvent

class AppleSkinClientEventHandler: AppleSkinApi {
    override fun registerEvents() {
        HUDOverlayEvent.Exhaustion.EVENT.register(::checkIsScanning)
        HUDOverlayEvent.Saturation.EVENT.register(::checkIsScanning)
        HUDOverlayEvent.HealthRestored.EVENT.register(::checkIsScanning)
        HUDOverlayEvent.HungerRestored.EVENT.register(::checkIsScanning)
    }

    private fun checkIsScanning(e: HUDOverlayEvent) {
        if (CobblemonClient.pokedexUsageContext.scanningGuiOpen) {
            e.isCanceled = true
        }
    }
}