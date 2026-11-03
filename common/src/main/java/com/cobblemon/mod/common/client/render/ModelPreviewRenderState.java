/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.cobblemon.mod.common.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** A frozen model preview in the same local pixel coordinates as the extracting GUI. */
public record ModelPreviewRenderState(
    ModelGeometry geometry,
    LightingMode lighting,
    Matrix3x2fc pose,
    int x0, int y0, int x1, int y1,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    @Override public float scale() { return 1; }

    public static void extract(GuiGraphicsExtractor graphics, ModelGeometry geometry) {
        extract(graphics, geometry, LightingMode.PROFILE);
    }

    public static void extract(GuiGraphicsExtractor graphics, ModelGeometry geometry, LightingMode lighting) {
        if (geometry.isEmpty()) return;
        // Pad for filtering at the silhouette; clipping is retained in absolute screen coordinates.
        int x0 = (int) Math.floor(geometry.minX()) - 1;
        int y0 = (int) Math.floor(geometry.minY()) - 1;
        int x1 = (int) Math.ceil(geometry.maxX()) + 1;
        int y1 = (int) Math.ceil(geometry.maxY()) + 1;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) graphics;
        ScreenRectangle scissor = accessor.cobblemon$getScissorStack().peek();
        ScreenRectangle bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, pose, scissor);
        if (bounds == null) return;
        accessor.cobblemon$getGuiRenderState().addPicturesInPictureState(
            new ModelPreviewRenderState(geometry, lighting, pose, x0, y0, x1, y1, scissor, bounds)
        );
    }

    public enum LightingMode { PORTRAIT, PROFILE, POKE_BALL }
}
