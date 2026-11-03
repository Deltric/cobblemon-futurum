/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.atlas

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.resources.Identifier

/** Descriptor registered with the native atlas manager; reload/upload ownership stays in Minecraft. */
class CobblemonAtlas(val textureId: Identifier, val sourcePath: Identifier) {
    val config get() = AtlasManager.AtlasConfig(textureId, sourcePath, false)
    val textureAtlas: TextureAtlas get() = Minecraft.getInstance().atlasManager.getAtlasOrThrow(sourcePath)
    fun getSprite(id: Identifier): TextureAtlasSprite = textureAtlas.getSprite(id)
}
