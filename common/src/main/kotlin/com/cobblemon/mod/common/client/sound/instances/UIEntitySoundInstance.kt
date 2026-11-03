/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.sound.instances

import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

/**
 * Plays a sound for a Pokémon rendered in a GUI, where there is no entity to take the sound source from.
 * Unlike [SimpleSoundInstance.forUI] this uses [SoundSource.NEUTRAL] so the Friendly Creatures slider applies.
 */
class UIEntitySoundInstance(sound: SoundEvent, volume: Float = 1F, pitch: Float = 1F) :
    SimpleSoundInstance(sound.location, SoundSource.NEUTRAL, volume, pitch, SoundInstance.createUnseededRandom(),
        false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true)
