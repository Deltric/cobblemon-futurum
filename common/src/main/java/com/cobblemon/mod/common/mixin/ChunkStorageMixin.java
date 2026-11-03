/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import static com.cobblemon.mod.common.datafixer.CobblemonSchemas.VERSION_KEY;

import com.cobblemon.mod.common.datafixer.CobblemonSchemas;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleRegionStorage.class)
public class ChunkStorageMixin {
    @Shadow @Final private DataFixTypes dataFixType;

    // This storage now also handles entities and POIs; only the chunk schema belongs here.
    @ModifyReturnValue(
        method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("RETURN")
    )
    private CompoundTag cobblemon$doChunkFix(CompoundTag vanillaFixed) {
        if (dataFixType != DataFixTypes.CHUNK) return vanillaFixed;
        int curVersion = vanillaFixed.getIntOr(VERSION_KEY, 0);
        return DataFixTypes.CHUNK.update(CobblemonSchemas.getDATA_FIXER(), vanillaFixed, curVersion, CobblemonSchemas.DATA_VERSION);
    }
}
