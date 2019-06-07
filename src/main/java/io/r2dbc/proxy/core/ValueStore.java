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

import reactor.util.annotation.Nullable;

import java.util.Map;

/**
 * Custom value store.
 *
 * @author Tadaya Tsuyukubo
 * @see ConnectionInfo
 * @see MethodExecutionInfo
 * @see QueryExecutionInfo
 */
public interface ValueStore {

    /**
     * Create default {@link ValueStore}.
     *
     * @return value store
     */
    static ValueStore create() {
        return new DefaultValueStore();
    }

    /**
     * Get the value associated to the key.
     *
     * @param key key
     * @return value; can be {@code null}
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @Nullable
    Object get(Object key);

    /**
     * Get the value associated to the key and cast to the type.
     *
     * @param key  key
     * @param type value type to cast
     * @param <T>  value type
     * @return value; can be {@code null}
     * @throws IllegalArgumentException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    @Nullable
    <T> T get(Object key, Class<T> type);

    /**
     * Get the value associated to the key; otherwise returns specified value.
     *
     * @param key          key
     * @param defaultValue default value
     * @param <T>          value type
     * @return value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @Nullable
    <T> T getOrDefault(Object key, T defaultValue);

    /**
     * Store a value associating the provided key.
     *
     * @param key   key
     * @param value value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    void put(Object key, Object value);

    /**
     * Store all key value pairs from provided map.
     *
     * @param map map
     * @throws IllegalArgumentException if {@code map} is {@code null}
     */
    void putAll(Map<Object, Object> map);

    /**
     * Remove the value associated to the provided key.
     *
     * @param key key
     * @return previously associated value or {@code null} if key did not exist
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @Nullable
    Object remove(Object key);

}
