/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.proxy.core;

/**
 * Represent a value for {@link io.r2dbc.spi.Statement#bind} and {@link io.r2dbc.spi.Statement#bindNull} operations.
 *
 * @author Tadaya Tsuyukubo
 */
public interface BindingValue {

    Object getValue();

    class NullBindingValue implements BindingValue {

        private Class<?> type;  // type of null

        public NullBindingValue(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object getValue() {
            return null;  // value is always null
        }

        public Class<?> getType() {
            return type;
        }
    }

    class SimpleBindingValue implements BindingValue {

        private Object value;

        public SimpleBindingValue(Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

    }

}
