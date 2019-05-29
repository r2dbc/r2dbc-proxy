/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.proxy.core;

import io.r2dbc.proxy.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link ValueStore}.
 *
 * <p> Simply uses {@link Map} as underlying storage.
 * Primitive values will be autoboxed to corresponding wrapper values when they
 * are stored.
 *
 * @author Tadaya Tsuyukubo
 */
public class DefaultValueStore implements ValueStore {

    private Map<Object, Object> map = new HashMap<>();

    @Override
    public Object get(Object key) {
        Assert.requireNonNull(key, "key must not be null");

        return this.map.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Assert.requireNonNull(key, "key must not be null");
        Assert.requireNonNull(type, "type must not be null");

        Object value = this.map.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(Object key, T defaultValue) {
        Assert.requireNonNull(key, "key must not be null");
        Assert.requireNonNull(defaultValue, "type must not be null");

        Object value = this.map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    @Override
    public void put(Object key, Object value) {
        Assert.requireNonNull(key, "key must not be null");

        this.map.put(key, value);
    }

    @Override
    public void putAll(Map<Object, Object> map) {
        Assert.requireNonNull(map, "map must not be null");

        this.map.putAll(map);
    }

    @Override
    public Object remove(Object key) {
        Assert.requireNonNull(key, "key must not be null");

        return this.map.remove(key);
    }
}
