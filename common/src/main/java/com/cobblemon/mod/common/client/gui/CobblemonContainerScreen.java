/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.client.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/** Keeps the nullable outside-slot contract used by vanilla container click dispatch. */
public abstract class CobblemonContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected CobblemonContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int button, ContainerInput input) {
        super.slotClicked(slot, slotId, button, input);
    }
}
