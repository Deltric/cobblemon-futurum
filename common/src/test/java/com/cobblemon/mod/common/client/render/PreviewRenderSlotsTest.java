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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewRenderSlotsTest {
    /** Models native prepare: resize/clear the owner's texture, then queue its view. */
    private static final class Renderer {
        private Target target;
        private int closes;

        Target prepare(String species, int width, int height) {
            assertEquals(0, closes);
            if (target == null || target.width != width || target.height != height) {
                if (target != null) target.close();
                target = new Target(width, height);
            }
            target.species = species;
            return target; // native BlitRenderState stores the live texture view
        }

        void close() {
            assertEquals(0, closes++);
            if (target != null) target.close();
        }
    }

    private static final class Target {
        private final int width;
        private final int height;
        private String species;
        private int closes;
        Target(int width, int height) { this.width = width; this.height = height; }
        String draw() { assertEquals(0, closes, "queued texture view was closed before drawing"); return species; }
        void close() { assertEquals(0, closes++); }
    }

    private static final class Previews {
        final List<Renderer> allocated = new ArrayList<>();
        final PreviewRenderSlots<Renderer> slots = new PreviewRenderSlots<>(() -> {
            var renderer = new Renderer();
            allocated.add(renderer);
            return renderer;
        }, Renderer::close);
    }

    @Test void deferredStarterBlitsRetainDistinctModelsAcrossSameSizeAndResizedPreviews() {
        var previews = new Previews();
        var blits = List.of(
            previews.slots.acquire().prepare("bulbasaur", 192, 184),
            previews.slots.acquire().prepare("bulbasaur", 36, 36),
            previews.slots.acquire().prepare("charmander", 36, 36),
            previews.slots.acquire().prepare("squirtle", 36, 36),
            previews.slots.acquire().prepare("cyndaquil", 34, 39));
        // All extraction/preparation finishes before any GUI element is drawn.
        assertEquals(List.of("bulbasaur", "bulbasaur", "charmander", "squirtle", "cyndaquil"),
            blits.stream().map(Target::draw).toList());
        assertEquals(5, blits.stream().distinct().count());
        assertEquals(192, blits.getFirst().width);
        assertEquals(184, blits.getFirst().height);
        previews.slots.endFrame();
        previews.slots.close();
        blits.forEach(target -> assertEquals(1, target.closes));
    }

    @Test void constantPopulationReusesOwnersAcrossFramesWithoutUnboundedAllocation() {
        var previews = new Previews();
        for (int frame = 0; frame < 200; frame++) {
            var blits = new ArrayList<Target>();
            for (int index = 0; index < 10; index++)
                blits.add(previews.slots.acquire().prepare("frame" + frame + "-pokemon" + index, 32 + frame % 2, 32));
            for (int index = 0; index < blits.size(); index++)
                assertEquals("frame" + frame + "-pokemon" + index, blits.get(index).draw());
            previews.slots.endFrame();
        }
        assertEquals(10, previews.allocated.size());
        assertTrue(previews.allocated.stream().allMatch(renderer -> renderer.closes == 0));
        previews.slots.close();
        previews.allocated.forEach(renderer -> assertEquals(1, renderer.closes));
    }

    @Test void shrinkingOrEmptyGuiReleasesUnusedOwnersAfterDrawAndReusesOnlyFinishedSlots() {
        var previews = new Previews();
        var first = previews.slots.acquire();
        var second = previews.slots.acquire();
        var firstBlit = first.prepare("bulbasaur", 36, 36);
        var secondBlit = second.prepare("charmander", 36, 36);
        assertEquals("bulbasaur", firstBlit.draw());
        assertEquals("charmander", secondBlit.draw());
        previews.slots.endFrame();
        assertSame(first, previews.slots.acquire());
        var nextBlit = first.prepare("squirtle", 80, 92);
        assertEquals(1, firstBlit.closes, "resize is safe only after the previous GUI draw");
        assertEquals(0, second.closes);
        assertEquals("squirtle", nextBlit.draw());
        previews.slots.endFrame();
        assertEquals(1, second.closes);
        assertEquals(0, first.closes);
        // A screen with no previews reaches the same draw-completion boundary.
        previews.slots.endFrame();
        assertEquals(1, first.closes);
        previews.slots.close();
        assertEquals(1, first.closes);
        assertEquals(1, second.closes);
    }

    @Test void rendererShutdownClosesAllActiveAndRetainedOwnersExactlyOnce() {
        var previews = new Previews();
        previews.slots.acquire().prepare("bulbasaur", 32, 32);
        previews.slots.endFrame();
        previews.slots.acquire().prepare("charmander", 32, 32);
        previews.slots.acquire().prepare("squirtle", 32, 32);
        previews.slots.close();
        previews.slots.close();
        previews.allocated.forEach(renderer -> assertEquals(1, renderer.closes));
    }
}
