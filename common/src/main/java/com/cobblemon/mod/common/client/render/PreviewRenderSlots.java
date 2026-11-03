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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A native PiP renderer owns one mutable render target. Reserve a distinct owner
 * for each deferred blit, then reuse owners only after the GUI has finished drawing.
 */
final class PreviewRenderSlots<T> implements AutoCloseable {
    private final List<T> slots = new ArrayList<>();
    private final Supplier<T> create;
    private final Consumer<T> dispose;
    private int used;

    PreviewRenderSlots(Supplier<T> create, Consumer<T> dispose) {
        this.create = create;
        this.dispose = dispose;
    }

    T acquire() {
        if (used == slots.size()) slots.add(create.get());
        return slots.get(used++);
    }

    /** Called after GuiRenderer.draw and GuiRenderState.reset, never during prepare. */
    void endFrame() {
        while (slots.size() > used) dispose.accept(slots.removeLast());
        used = 0;
    }

    @Override public void close() {
        while (!slots.isEmpty()) dispose.accept(slots.removeLast());
        used = 0;
    }
}
