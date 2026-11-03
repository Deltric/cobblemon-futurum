/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.Projection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewRenderResolutionTest {
    @Test void unscaledHudRetainsItsFramebufferResolution() {
        for (int scale = 1; scale <= 6; scale++) {
            assertEquals(scale, PreviewRenderResolution.textureScale(new Matrix3x2f().translate(180, 40), scale));
        }
    }

    @Test void enlargedPcModelsAreRasterizedAtTheirDisplayedSize() {
        var portrait = new Matrix3x2f().translate(180, 100).scale(2);
        assertEquals(6, PreviewRenderResolution.textureScale(portrait, 3));
        // PCGUI uses a 2x ModelWidget. A 20-unit silhouette occupies 120
        // framebuffer pixels, not a 60-pixel image enlarged by the GUI blit.
        assertEquals(120, 20 * PreviewRenderResolution.textureScale(portrait, 3));
        assertEquals(8, PreviewRenderResolution.textureScale(new Matrix3x2f().scale(2.5F), 3));
        assertEquals(9, PreviewRenderResolution.textureScale(new Matrix3x2f().scale(2.7F), 3));
    }

    @Test void fractionalAndReflectedWidgetsRemainSharpWithoutChangingTheirPlacement() {
        var pose = new Matrix3x2f().translate(73, 91).scale(-1.25F, 0.5F);
        var unchanged = new Matrix3x2f(pose);
        assertEquals(4, PreviewRenderResolution.textureScale(pose, 3));
        assertEquals(unchanged, pose);
        assertEquals(2, PreviewRenderResolution.textureScale(new Matrix3x2f().scale(0.5F), 3));
    }

    @Test void rotationAndShearCannotStretchATexelBeyondOneFramebufferPixel() {
        for (var pose : new Matrix3x2f[] {
                new Matrix3x2f().rotate(0.7F),
                new Matrix3x2f().rotate(0.7F).scale(4, 2),
                new Matrix3x2f(1, 0, 1, 1, 50, 70)}) {
            int textureScale = PreviewRenderResolution.textureScale(pose, 3);
            for (int degree = 0; degree < 360; degree++) {
                double radians = Math.toRadians(degree);
                var texel = new Vector2f((float) Math.cos(radians), (float) Math.sin(radians)).div(textureScale);
                pose.transformDirection(texel);
                assertTrue(texel.length() * 3 <= 1.000001, "A rendered texel is enlarged on screen");
            }
        }
    }

    @Test void sharperTexturesRetainOriginalClipPositionsIncludingDeepGeometry() {
        for (int scale : new int[] {2, 3, 6, 8, 9, 12}) {
            var projection = new Projection();
            PreviewRenderResolution.setupProjection(projection, 80 * scale, 60 * scale, 3, scale);
            assertEquals(80 * scale, projection.width());
            assertEquals(60 * scale, projection.height());
            assertTrue(projection.invertY());
            // These are the exact two native orthographic conventions. Do not
            // require a GPU device just to select its depth convention.
            for (boolean zeroToOne : new boolean[] {false, true}) {
                var oldProjection = new Matrix4f().setOrtho(0, 240, 180, 0, 1000, -1000, zeroToOne);
                var sharperProjection = new Matrix4f().setOrtho(0, projection.width(), projection.height(), 0,
                    projection.zFar(), projection.zNear(), zeroToOne);
                for (float z : new float[] {-400, -300, -80, 0, 300, 400}) {
                    var original = oldProjection.transformPosition(new Vector3f(34, 27, z).mul(3));
                    var sharper = sharperProjection.transformPosition(new Vector3f(34, 27, z).mul(scale));
                    assertTrue(original.equals(sharper, 0.000001F), "Raster resolution changed clip coordinates");
                }
            }
        }
    }
}
