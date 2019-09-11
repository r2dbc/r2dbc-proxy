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

    private final SortedSet<Binding> namedBindings = new TreeSet<>();

    /**
     * Add an index binding.
     *
     * @param index      index
     * @param boundValue boundValue
     * @throws IllegalArgumentException if {@code boundValue} is {@code null}
     */
    public void addIndexBinding(int index, BoundValue boundValue) {
        Assert.requireNonNull(boundValue, "boundValue must not be null");

        addIndexBinding(new IndexBinding(index, boundValue));
    }

    /**
     * Add an index binding.
     *
     * @param indexBinding indexBinding
     * @throws IllegalArgumentException if {@code indexBinding} is {@code null}
     */
    public void addIndexBinding(IndexBinding indexBinding) {
        Assert.requireNonNull(indexBinding, "indexBinding must not be null");

        this.indexBindings.add(indexBinding);
    }

    /**
     * Add a named binding.
     *
     * @param name       index name
     * @param boundValue boundValue
     * @throws IllegalArgumentException if {@code boundValue} is {@code null}
     */
    public void addNamedBinding(String name, BoundValue boundValue) {
        Assert.requireNonNull(boundValue, "boundValue must not be null");

        addNamedBinding(new NamedBinding(name, boundValue));
    }

    /**
     * Add a named binding.
     *
     * @param namedBinding namedBinding
     * @throws IllegalArgumentException if {@code namedBinding} is {@code null}
     */
    public void addNamedBinding(NamedBinding namedBinding) {
        Assert.requireNonNull(namedBinding, "namedBinding must not be null");

        this.namedBindings.add(namedBinding);
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
     * Get a sorted set of named bindings.
     *
     * @return named bindings
     */
    public SortedSet<Binding> getNamedBindings() {
        return this.namedBindings;
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
     * Represents named(String) binding.
     */
    public static class NamedBinding implements Binding, Comparable<NamedBinding> {

        private String name;

        private BoundValue boundValue;

        public NamedBinding(String name, BoundValue boundValue) {
            Assert.requireNonNull(name, "name must not be null");
            Assert.requireNonNull(boundValue, "boundValue must not be null");

            this.name = name;
            this.boundValue = boundValue;
        }

        @Override
        public int compareTo(@NonNull NamedBinding o) {
            // name is non-null
            return Objects.compare(this.name, o.name, naturalOrder());
        }

        @Override
        public Object getKey() {
            return this.name;
        }

        @Override
        public BoundValue getBoundValue() {
            return this.boundValue;
        }
    }

}
