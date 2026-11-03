/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

/**
 * Owns pending CPU images until upload, then owns their texture registrations until
 * drawing ends. Slot identities are reused only after that boundary, so native
 * material caches grow with the largest frame rather than the number of frames.
 */
final class FrameTextureSlots<T> {
    private record Pending<T>(Identifier id, T image) {}
    private final ArrayDeque<Pending<T>> pending = new ArrayDeque<>();
    private final List<Identifier> uploaded = new ArrayList<>();
    private final BiConsumer<Identifier, T> upload;
    private final Consumer<Identifier> release;
    private final Consumer<T> discard;
    private int nextSlot;

    FrameTextureSlots(BiConsumer<Identifier, T> upload, Consumer<Identifier> release, Consumer<T> discard) {
        this.upload = upload;
        this.release = release;
        this.discard = discard;
    }

    synchronized Identifier register(T image) {
        Identifier id = Identifier.fromNamespaceAndPath("cobblemon", "dynamic/model_interpolation/" + nextSlot++);
        pending.addLast(new Pending<>(id, image));
        return id;
    }

    synchronized void upload() {
        while (!pending.isEmpty()) {
            Pending<T> next = pending.getFirst();
            // A successful callback takes ownership of the CPU image. If it
            // throws, the image remains pending for cleanup at frame end.
            upload.accept(next.id, next.image);
            pending.removeFirst();
            uploaded.add(next.id);
        }
    }

    synchronized void endFrame() {
        for (Identifier id : uploaded) release.accept(id);
        uploaded.clear();
        while (!pending.isEmpty()) discard.accept(pending.removeFirst().image);
        nextSlot = 0;
    }
}
