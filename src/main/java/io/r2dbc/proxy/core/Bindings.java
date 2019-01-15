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


import io.r2dbc.proxy.util.Assert;
import reactor.util.annotation.NonNull;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Comparator.naturalOrder;

/**
 * Represent set of {@link Binding} for each batch.
 *
 * @author Tadaya Tsuyukubo
 */
public class Bindings {

    private final SortedSet<Binding> indexBindings = new TreeSet<>();

    private final SortedSet<Binding> identifierBindings = new TreeSet<>();

    /**
     * Add an index binding.
     *
     * @param index      index
     * @param boundValue boundValue
     * @throws IllegalArgumentException if {@code boundValue} is {@code null}
     */
    public void addIndexBinding(int index, BoundValue boundValue) {
        Assert.requireNonNull(boundValue, "boundValue must not be null");

        this.indexBindings.add(new IndexBinding(index, boundValue));
    }

    /**
     * Add an identifier binding.
     *
     * @param identifier identifier
     * @param boundValue boundValue
     * @throws IllegalArgumentException if {@code identifier} is {@code null}
     * @throws IllegalArgumentException if {@code boundValue} is {@code null}
     */
    public void addIdentifierBinding(Object identifier, BoundValue boundValue) {
        Assert.requireNonNull(identifier, "identifier must not be null");
        Assert.requireNonNull(boundValue, "boundValue must not be null");

        this.identifierBindings.add(new IdentifierBinding(identifier, boundValue));
    }

    /**
     * Get a sorted set of index bindings.
     *
     * @return index bindings
     */
    public SortedSet<Binding> getIndexBindings() {
        return this.indexBindings;
    }

    /**
     * Get a sorted set of identifier bindings.
     *
     * @return identifier bindings
     */
    public SortedSet<Binding> getIdentifierBindings() {
        return this.identifierBindings;
    }

    /**
     * Represents index binding.
     */
    public static class IndexBinding implements Binding, Comparable<IndexBinding> {

        private int index;

        private BoundValue boundValue;

        public IndexBinding(int index, BoundValue boundValue) {
            Assert.requireNonNull(boundValue, "boundValue must not be null");

            this.index = index;
            this.boundValue = boundValue;
        }

        @Override
        public int compareTo(@NonNull IndexBinding o) {
            return Integer.compare(this.index, o.index);
        }

        @Override
        public Object getKey() {
            return this.index;
        }

        @Override
        public BoundValue getBoundValue() {
            return this.boundValue;
        }
    }

    /**
     * Represents identifier binding.
     */
    public static class IdentifierBinding implements Binding, Comparable<IdentifierBinding> {

        private Object identifier;

        private BoundValue boundValue;

        public IdentifierBinding(Object identifier, BoundValue boundValue) {
            Assert.requireNonNull(identifier, "identifier must not be null");
            Assert.requireNonNull(boundValue, "boundValue must not be null");

            this.identifier = identifier;
            this.boundValue = boundValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compareTo(@NonNull IdentifierBinding o) {
            return Objects.compare((Comparable) this.identifier, (Comparable) o.identifier, naturalOrder());
        }

        @Override
        public Object getKey() {
            return this.identifier;
        }

        @Override
        public BoundValue getBoundValue() {
            return this.boundValue;
        }

    }

}
