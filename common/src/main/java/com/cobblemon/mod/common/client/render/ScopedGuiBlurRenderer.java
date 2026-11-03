/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jspecify.annotations.Nullable;

/** Restores unblurred pixels outside the tooltip after Minecraft's native blur pass. */
public final class ScopedGuiBlurRenderer implements AutoCloseable {
    private @Nullable GpuTexture backup;

    public void render(GameRenderer renderer, GuiRenderState state, Runnable blur) {
        var regions = ((ScopedGuiBlur.State) state).cobblemon$blurRegions();
        if (regions.isEmpty()) {
            blur.run();
            return;
        }
        GpuTexture target = renderer.mainRenderTarget().getColorTexture();
        if (target == null) return;
        int width = target.getWidth(0);
        int height = target.getHeight(0);
        if (backup == null || backup.getWidth(0) != width || backup.getHeight(0) != height || backup.getFormat() != target.getFormat()) {
            close();
            backup = RenderSystem.getDevice().createTexture(() -> "Cobblemon scoped GUI blur backup",
                GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST, target.getFormat(), width, height, 1, 1);
        }
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(target, backup, 0, 0, 0, 0, 0, width, height);
        blur.run();
        encoder = RenderSystem.getDevice().createCommandEncoder();
        for (ScreenRectangle area : GuiBlurRegions.outsideRegions(width, height, renderer.gameRenderState().windowRenderState.guiScale, regions)) {
            encoder.copyTextureToTexture(backup, target, 0, area.left(), area.top(), area.left(), area.top(), area.width(), area.height());
        }
    }

    @Override public void close() {
        if (backup != null) {
            backup.close();
            backup = null;
        }
    }
}
