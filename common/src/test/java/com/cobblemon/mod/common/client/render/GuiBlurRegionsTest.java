/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiBlurRegionsTest {
    private void checkPixels(int width, int height, int scale, List<ScreenRectangle> regions) {
        var restore = GuiBlurRegions.outsideRegions(width, height, scale, regions);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean blurred = false;
                for (var area : regions) {
                    blurred |= x >= (long) area.left() * scale && x < (long) area.right() * scale
                        && y >= height - (long) area.bottom() * scale && y < height - (long) area.top() * scale;
                }
                int copies = 0;
                for (var area : restore) {
                    if (x >= area.left() && x < area.right() && y >= area.top() && y < area.bottom()) copies++;
                }
                assertEquals(blurred ? 0 : 1, copies, "Pixel " + x + "," + y);
            }
        }
    }

    @Test void scaledTooltipPreservesEveryOutsidePixelAndDoesNotRestoreInsidePixels() {
        checkPixels(80, 60, 2, List.of(new ScreenRectangle(7, 3, 12, 9)));
    }

    @Test void overlappingAndOffscreenRegionsUseTheirUnionWithoutDuplicateRestoration() {
        checkPixels(80, 60, 3, List.of(new ScreenRectangle(-4, 2, 18, 13), new ScreenRectangle(9, 8, 15, 15), new ScreenRectangle(21, -4, 20, 12)));
    }

    @Test void EmptyAndFullScreenScopesRespectTheFramebufferEdges() {
        checkPixels(40, 30, 1, List.of());
        checkPixels(40, 30, 1, List.of(new ScreenRectangle(-10, -10, 100, 100)));
        checkPixels(40, 30, 1, List.of(new ScreenRectangle(100, 100, 10, 10)));
    }
}
