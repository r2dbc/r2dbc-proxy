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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tadaya Tsuyukubo
 */
public class DefaultValueStoreTest {

    @Test
    void get() {
        DefaultValueStore store = new DefaultValueStore();

        store.put("foo", "FOO");

        assertThat(store.get("foo")).isEqualTo("FOO");
        assertThat(store.get("foo", String.class)).isEqualTo("FOO");

        store.put("foo", "FOO-UPDATED");
        assertThat(store.get("foo")).isEqualTo("FOO-UPDATED");

        assertThat(store.get("bar")).isNull();
        assertThat(store.get("bar", String.class)).isNull();

        assertThatThrownBy(() -> store.get("foo", int.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    @Test
    void remove() {
        DefaultValueStore store = new DefaultValueStore();

        store.put("foo", "FOO");

        assertThat(store.remove("foo")).isEqualTo("FOO");
        assertThat(store.remove("foo")).isNull();
    }

    @Test
    void autoboxing() {
        DefaultValueStore store = new DefaultValueStore();

        store.put("foo", 100);

        store.get("foo", Integer.class);

        assertThatThrownBy(() -> store.get("foo", int.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }
}
