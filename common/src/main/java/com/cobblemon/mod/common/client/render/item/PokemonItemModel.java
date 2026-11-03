/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Extracts a species-dependent model before the native item render state is submitted. */
public final class PokemonItemModel implements ItemModel {
    private final PokemonItemRenderer renderer = new PokemonItemRenderer();
    private final Matrix4fc transformation;

    public PokemonItemModel(Matrix4fc transformation) {
        this.transformation = new Matrix4f(transformation);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        PokemonItemRenderer.Snapshot snapshot = renderer.extractGeometry(item, displayContext);
        if (snapshot == null) return;
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(snapshot);
        output.setAnimated();
        var layer = output.newLayer();
        layer.setExtents(snapshot.getGeometry()::extents);
        layer.setLocalTransform(transformation);
        layer.setUsesBlockLight(displayContext != ItemDisplayContext.GUI);
        layer.setupSpecialModel(renderer, snapshot);
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
        @Override public MapCodec<Unbaked> type() { return MAP_CODEC; }
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) {
            // The renderer resolves its Bedrock model from the reloaded Cobblemon repository at extraction time.
        }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new PokemonItemModel(transformation);
        }
    }
}
