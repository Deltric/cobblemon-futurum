/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameTextureSlotsTest {
    private static final class Image {
        private int closeCount;
        void close() { assertEquals(0, closeCount++); }
    }

    private static final class Textures {
        private final Map<Identifier, Image> owned = new HashMap<>();
        private final List<Image> discarded = new ArrayList<>();
        private final FrameTextureSlots<Image> slots = new FrameTextureSlots<>((id, image) -> {
            assertEquals(0, image.closeCount);
            assertNull(owned.put(id, image), "an image still used by this frame must never be replaced");
        }, id -> {
            Image image = owned.remove(id);
            assertNotNull(image);
            image.close();
        }, image -> {
            discarded.add(image);
            image.close();
        });
    }

    @Test void differentSnapshotsKeepTheirOwnImagesUntilFrameEndThenReuseReleasedSlots() {
        var textures = new Textures();
        var first = new Image();
        var second = new Image();
        var firstId = textures.slots.register(first);
        textures.slots.upload();
        var secondId = textures.slots.register(second);
        assertNotEquals(firstId, secondId);
        textures.slots.upload();
        assertSame(first, textures.owned.get(firstId));
        assertSame(second, textures.owned.get(secondId));
        assertEquals(0, first.closeCount);
        assertEquals(0, second.closeCount);
        textures.slots.endFrame();
        assertTrue(textures.owned.isEmpty());
        assertEquals(1, first.closeCount);
        assertEquals(1, second.closeCount);
        var next = new Image();
        assertEquals(firstId, textures.slots.register(next));
        textures.slots.upload();
        assertSame(next, textures.owned.get(firstId));
        textures.slots.endFrame();
        assertEquals(1, next.closeCount);
        assertTrue(textures.discarded.isEmpty());
    }

    @Test void nativeOutlineCacheGrowthIsBoundedByPeakFramePopulation() {
        var textures = new Textures();
        var ids = new HashSet<Identifier>();
        var outlines = new HashSet<RenderType>();
        int peak = 7;
        for (int frame = 0; frame < 100; frame++) {
            int count = 1 + frame % peak;
            for (int slot = 0; slot < count; slot++) {
                Identifier id = textures.slots.register(new Image());
                ids.add(id);
                // This native factory exercises the same permanent outline
                // memoization used by the interpolated material constructor.
                outlines.add(RenderTypes.entityCutoutCull(id).outline().orElseThrow());
            }
            textures.slots.upload();
            assertEquals(count, textures.owned.size());
            textures.slots.endFrame();
            assertTrue(textures.owned.isEmpty());
        }
        assertEquals(peak, ids.size());
        assertEquals(peak, outlines.size());
    }

    @Test void unuploadedImagesAreClosedWithoutReleasingSomeoneElsesTexture() {
        var textures = new Textures();
        var uploaded = new Image();
        textures.slots.register(uploaded);
        textures.slots.upload();
        var pending = new Image();
        textures.slots.register(pending);
        textures.slots.endFrame();
        textures.slots.endFrame();
        textures.slots.upload();
        assertEquals(1, uploaded.closeCount);
        assertEquals(1, pending.closeCount);
        assertEquals(List.of(pending), textures.discarded);
        assertTrue(textures.owned.isEmpty());
    }

    @Test void panoramaUploadsAndRepeatedNormalUploadsKeepWorldAndGuiImagesAliveUntilFrameEnd() {
        var textures = new Textures();
        var images = new ArrayList<Image>();
        var ids = new HashSet<Identifier>();
        // Each panorama face extracts independently and enters renderLevel
        // without entering render. No face may overwrite an earlier image.
        for (int face = 0; face < 6; face++) {
            var image = new Image();
            images.add(image);
            assertTrue(ids.add(textures.slots.register(image)));
            textures.slots.upload();
            assertEquals(face + 1, textures.owned.size());
            images.forEach(it -> assertEquals(0, it.closeCount));
        }
        // Normal render HEAD uploads GUI extraction too; the nested
        // renderLevel HEAD upload is idempotent and must not release it.
        var gui = new Image();
        var guiId = textures.slots.register(gui);
        assertTrue(ids.add(guiId));
        textures.slots.upload();
        textures.slots.upload();
        assertSame(gui, textures.owned.get(guiId));
        assertEquals(0, gui.closeCount);
        images.forEach(it -> assertEquals(0, it.closeCount));
        textures.slots.endFrame();
        images.forEach(it -> assertEquals(1, it.closeCount));
        assertEquals(1, gui.closeCount);
        assertTrue(textures.owned.isEmpty());
    }
}
