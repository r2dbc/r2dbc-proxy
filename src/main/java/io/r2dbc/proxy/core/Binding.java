/*
 * Copyright 2018 the original author or authors.
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

/**
 * Represent an operation of {@link io.r2dbc.spi.Statement#bind} and {@link io.r2dbc.spi.Statement#bindNull}.
 *
 * @author Tadaya Tsuyukubo
 * @see Bindings.IndexBinding
 * @see Bindings.NamedBinding
 */
public interface Binding {

    /**
     * Get a key which represents index or name of the binding.
     *
     * @return an index or name
     */
    Object getKey();

    /**
     * Get a {@link BoundValue}.
     *
     * @return a bound value
     */
    BoundValue getBoundValue();

}
