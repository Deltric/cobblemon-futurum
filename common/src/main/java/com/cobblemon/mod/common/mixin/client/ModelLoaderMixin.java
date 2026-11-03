/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Retains add-on discovery of egg/TM sprites while allowing explicit 26.3 item definitions to win. */
@Mixin(ClientItemInfoLoader.class)
public abstract class ModelLoaderMixin {
    @Inject(method = "scheduleLoad", at = @At("RETURN"), cancellable = true)
    private static void cobblemon$dynamicItemDefinitions(ResourceManager manager, Executor executor,
            CallbackInfoReturnable<CompletableFuture<ClientItemInfoLoader.LoadedClientInfos>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApplyAsync(loaded -> {
            var models = new HashMap<>(loaded.contents());
            for (String folder : List.of("egg", "tm")) {
                manager.listResources("models/item/" + folder, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
                    String path = file.getPath().substring("models/".length(), file.getPath().length() - ".json".length());
                    Identifier cuboidId = file.withPath(path);
                    Identifier itemId = file.withPath(path.substring("item/".length()));
                    models.putIfAbsent(itemId, new ClientItem(
                            new CuboidItemModelWrapper.Unbaked(cuboidId, Optional.empty(), List.of()), ClientItem.Properties.DEFAULT));
                });
            }
            return new ClientItemInfoLoader.LoadedClientInfos(models);
        }, executor));
    }
}
