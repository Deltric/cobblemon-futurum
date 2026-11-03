/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import net.minecraft.client.renderer.Projection;
import org.joml.Matrix3x2fc;

/** Keeps model textures at framebuffer resolution after the GUI's final transform. */
final class PreviewRenderResolution {
    private PreviewRenderResolution() {}

    static int textureScale(Matrix3x2fc pose, int guiScale) {
        // Native PiP sizes its texture in local GUI coordinates, but applies
        // this matrix only when blitting. Account for its greatest stretch so
        // enlarged widgets do not magnify an already rasterized small model.
        double a = pose.m00(), b = pose.m01(), c = pose.m10(), d = pose.m11();
        double first = a * a + b * b;
        double second = c * c + d * d;
        double cross = a * c + b * d;
        double stretch = Math.sqrt((first + second + Math.hypot(first - second, 2 * cross)) / 2);
        return Math.max(1, (int) Math.ceil(guiScale * stretch));
    }

    static void setupProjection(Projection projection, int width, int height, int guiScale, int textureScale) {
        // Native PiP also scales scene depth by its rasterization scale. Expand
        // the depth interval equally to retain clipping, depth and lighting
        // while increasing only the number of rasterized screen pixels.
        float depth = 1000F * textureScale / guiScale;
        projection.setupOrtho(-depth, depth, width, height, true);
    }
}
