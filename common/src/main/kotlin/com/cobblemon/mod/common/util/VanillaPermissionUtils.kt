/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.PermissionSetSupplier
import net.minecraft.world.entity.player.Player

/** Numeric levels remain a Cobblemon API input; translate them to native command permissions. */
fun PermissionSetSupplier.hasCommandPermission(level: Int): Boolean = permissions().hasCommandPermission(level)

fun Player.hasCommandPermission(level: Int): Boolean = permissions().hasCommandPermission(level)

internal fun PermissionSet.hasCommandPermission(level: Int): Boolean = when {
    level <= 0 -> true
    level > 4 -> false
    else -> hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(level)))
}
