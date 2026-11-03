/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge;

import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Client mirrors only; vanilla ServerPlayer remains the owner of saved shoulder tags. */
public final class NeoForgeShoulderAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Cobblemon.MODID);
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> LEFT =
        TYPES.register("shoulder_left", () -> AttachmentType.builder(() -> new CompoundTag())
            .sync(ByteBufCodecs.COMPOUND_TAG).build());
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> RIGHT =
        TYPES.register("shoulder_right", () -> AttachmentType.builder(() -> new CompoundTag())
            .sync(ByteBufCodecs.COMPOUND_TAG).build());

    private NeoForgeShoulderAttachments() {}

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    public static CompoundTag getLeft(Player player) {
        return player instanceof ServerPlayer serverPlayer
            ? serverPlayer.getShoulderEntityLeft() : player.getData(LEFT);
    }

    public static CompoundTag getRight(Player player) {
        return player instanceof ServerPlayer serverPlayer
            ? serverPlayer.getShoulderEntityRight() : player.getData(RIGHT);
    }

    public static void setLeft(Player player, CompoundTag tag) {
        set(player, LEFT, tag);
    }

    public static void setRight(Player player, CompoundTag tag) {
        set(player, RIGHT, tag);
    }

    private static void set(Player player, DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> type, CompoundTag tag) {
        // Loading has no connection, and restoreFrom runs before the replacement
        // player's final entity ID and level membership are assigned. NeoForge
        // immediately sends setData updates to the owner, so defer until login/respawn.
        if (player instanceof ServerPlayer serverPlayer
            && (serverPlayer.connection == null || !serverPlayer.isAddedToLevel())) {
            return;
        }
        player.setData(type, tag.copy());
    }

    /** Login/respawn events follow connection setup and the native initial sync. */
    public static void syncFromNative(ServerPlayer player) {
        setLeft(player, player.getShoulderEntityLeft());
        setRight(player, player.getShoulderEntityRight());
    }
}
