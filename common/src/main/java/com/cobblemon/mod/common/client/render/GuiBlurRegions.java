/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class GuiBlurRegions {
    private GuiBlurRegions() {}

    /** Pixel rectangles to restore after blur, excluding the union of all requested regions. */
    public static List<ScreenRectangle> outsideRegions(int width, int height, int guiScale, List<ScreenRectangle> regions) {
        List<ScreenRectangle> outside = List.of(new ScreenRectangle(0, 0, width, height));
        for (ScreenRectangle region : regions) {
            int left = clamp((long) region.left() * guiScale, width);
            int right = clamp((long) region.right() * guiScale, width);
            int bottom = height - clamp((long) region.bottom() * guiScale, height);
            int top = height - clamp((long) region.top() * guiScale, height);
            if (right <= left || top <= bottom) continue;
            ScreenRectangle cut = new ScreenRectangle(left, bottom, right - left, top - bottom);
            List<ScreenRectangle> remaining = new ArrayList<>();
            for (ScreenRectangle rectangle : outside) {
                ScreenRectangle intersection = rectangle.intersection(cut);
                if (intersection == null) {
                    remaining.add(rectangle);
                    continue;
                }
                add(remaining, rectangle.left(), rectangle.top(), rectangle.right(), intersection.top());
                add(remaining, rectangle.left(), intersection.bottom(), rectangle.right(), rectangle.bottom());
                add(remaining, rectangle.left(), intersection.top(), intersection.left(), intersection.bottom());
                add(remaining, intersection.right(), intersection.top(), rectangle.right(), intersection.bottom());
            }
            outside = remaining;
        }
        return List.copyOf(outside);
    }

    private static int clamp(long value, int max) { return (int) Math.max(0, Math.min(max, value)); }

    private static void add(List<ScreenRectangle> rectangles, int x0, int y0, int x1, int y1) {
        if (x1 > x0 && y1 > y0) rectangles.add(new ScreenRectangle(x0, y0, x1 - x0, y1 - y0));
    }
}
