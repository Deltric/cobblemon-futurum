/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Vector3f;

/** Extracted rectangular quads; no particle, runtime, or mutable input vector survives extraction. */
public final class SnowstormQuadRenderState extends QuadParticleRenderState {
    private final SingleQuadParticle.Layer directLayer;
    private final SingleQuadParticle.Layer afterTerrainLayer;
    private SingleQuadParticle.Layer layer;
    private final int order;
    private final List<Quad> quads = new ArrayList<>();

    public SnowstormQuadRenderState(SingleQuadParticle.Layer layer, int order) {
        this.directLayer = layer;
        this.afterTerrainLayer = new SingleQuadParticle.Layer(true, layer.textureAtlasLocation(), layer.pipeline());
        this.layer = layer;
        this.order = order;
    }

    public void addQuad(Vector3f[] vertices, float minU, float maxU, float minV, float maxV, int color, int light) {
        if (vertices.length != 4) throw new IllegalArgumentException("Particle quads require four vertices");
        float[] positions = new float[12];
        for (int i = 0; i < 4; i++) {
            positions[i * 3] = vertices[i].x;
            positions[i * 3 + 1] = vertices[i].y;
            positions[i * 3 + 2] = vertices[i].z;
        }
        quads.add(new Quad(positions, minU, maxU, minV, maxV, color, light));
    }

    @Override public void clear() { quads.clear(); }
    @Override public boolean isEmpty() { return quads.isEmpty(); }
    @Override public Set<SingleQuadParticle.Layer> layers() { return Set.of(layer); }
    @Override public void buildLayer(SingleQuadParticle.Layer requestedLayer, VertexConsumer consumer) {
        if (requestedLayer != layer) return;
        for (Quad quad : quads) {
            for (int i = 0; i < 4; i++) {
                consumer.addVertex(quad.positions[i * 3], quad.positions[i * 3 + 1], quad.positions[i * 3 + 2])
                    .setUv(i < 2 ? quad.maxU : quad.minU, i == 0 || i == 3 ? quad.maxV : quad.minV)
                    .setColor(quad.color).setLight(quad.light);
            }
        }
    }
    @Override public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
        if (!isEmpty()) {
            OrderedSubmitNodeCollector ordered = collector.order(order);
            // Regular transparency has a direct after-terrain pass. With OIT enabled that
            // phase aliases the accumulation pass, which cannot preserve these blend equations.
            layer = ordered instanceof SubmitNodeCollection collection && collection.afterTerrain != collection.oitTranslucent
                ? afterTerrainLayer : directLayer;
            ordered.submitQuadParticleGroup(this);
        }
    }
    private record Quad(float[] positions, float minU, float maxU, float minV, float maxV, int color, int light) {}
}
