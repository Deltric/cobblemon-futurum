/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Retains the lowercase enum JSON format used by interactions and evolution requirements. */
public final class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.getRawType().isEnum()) {
            return null;
        }
        Map<String, T> values = new HashMap<>();
        for (Object value : type.getRawType().getEnumConstants()) {
            values.put(((Enum<?>) value).name().toLowerCase(Locale.ROOT), (T) value);
        }
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter output, T value) throws IOException {
                output.value(((Enum<?>) value).name().toLowerCase(Locale.ROOT));
            }

            @Override
            public T read(JsonReader input) throws IOException {
                return values.get(input.nextString());
            }
        }.nullSafe();
    }
}
