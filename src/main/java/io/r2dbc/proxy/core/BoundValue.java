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

import io.r2dbc.proxy.util.Assert;

/**
 * Represent a value for {@link io.r2dbc.spi.Statement#bind} and {@link io.r2dbc.spi.Statement#bindNull} operations.
 *
 * @author Tadaya Tsuyukubo
 */
public interface BoundValue {

    /**
     * Create a {@link BoundValue} that represents {@link io.r2dbc.spi.Statement#bind}.
     *
     * @param value value
     * @return a boundValue
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    static BoundValue value(Object value) {
        Assert.requireNonNull(value, "value must not be null");

        DefaultBoundValue boundValue = new DefaultBoundValue();
        boundValue.value = value;
        return boundValue;
    }

    /**
     * Create a {@link BoundValue} that represents {@link io.r2dbc.spi.Statement#bindNull}.
     *
     * @param nullType {@code null} type
     * @return a boundValue
     * @throws IllegalArgumentException if {@code nullType} is {@code null}
     */
    static BoundValue nullValue(Class<?> nullType) {
        Assert.requireNonNull(nullType, "nullType must not be null");

        DefaultBoundValue boundValue = new DefaultBoundValue();
        boundValue.nullType = nullType;
        return boundValue;
    }

    /**
     * Distinguish between the bound value is for {@link io.r2dbc.spi.Statement#bind} or {@link io.r2dbc.spi.Statement#bindNull} operation.
     *
     * @return {@code true} when this represents value of {@link io.r2dbc.spi.Statement#bindNull} operation
     */
    boolean isNull();

    /**
     * Get the bound value by {@link io.r2dbc.spi.Statement#bind}.
     *
     * @return bound value
     */
    Object getValue();

    /**
     * Get the bound {@code null} type by {@link io.r2dbc.spi.Statement#bindNull}.
     *
     * @return {@code null} type
     */
    Class<?> getNullType();

    /**
     * Default implementation of {@link BoundValue}.
     */
    final class DefaultBoundValue implements BoundValue {

        private Object value;

        private Class<?> nullType;

        @Override
        public boolean isNull() {
            return this.nullType != null;
        }

        @Override
        public Class<?> getNullType() {
            return this.nullType;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

    }

}
