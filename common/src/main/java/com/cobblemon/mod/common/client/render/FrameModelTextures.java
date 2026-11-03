/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Owns interpolated layer textures until both world and GUI draw submission have finished. */
public final class FrameModelTextures {
    private static final FrameTextureSlots<NativeImage> SLOTS = new FrameTextureSlots<>((id, image) -> {
        var texture = new DynamicTexture(() -> "Cobblemon interpolated model layer", image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }, id -> Minecraft.getInstance().getTextureManager().release(id), NativeImage::close);

    private FrameModelTextures() {}

    /** Takes ownership of the interpolated CPU image without touching the GPU during extraction. */
    public static Identifier register(NativeImage image) {
        return SLOTS.register(image);
    }

    public static void upload() {
        SLOTS.upload();
    }

    public static void endFrame() {
        SLOTS.endFrame();
    }
}
