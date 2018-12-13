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

    private SortedSet<Binding> indexBindings = new TreeSet<>();

    private SortedSet<Binding> identifierBindings = new TreeSet<>();

    public void addIndexBinding(int index, BindingValue value) {
        this.indexBindings.add(new IndexBinding(index, value));
    }

    public void addIdentifierBinding(Object identifier, BindingValue value) {
        this.identifierBindings.add(new IdentifierBinding(identifier, value));
    }

    public SortedSet<Binding> getIndexBindings() {
        return indexBindings;
    }

    public SortedSet<Binding> getIdentifierBindings() {
        return identifierBindings;
    }

    public static class IndexBinding implements Binding, Comparable<IndexBinding> {

        private int index;

        private BindingValue value;

        public IndexBinding(int index, BindingValue value) {
            this.index = index;
            this.value = value;
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
        public BindingValue getBindingValue() {
            return this.value;
        }
    }

    public static class IdentifierBinding implements Binding, Comparable<IdentifierBinding> {

        private Object identifier;

        private BindingValue value;

        public IdentifierBinding(Object identifier, BindingValue value) {
            this.identifier = identifier;
            this.value = value;
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
        public BindingValue getBindingValue() {
            return this.value;
        }

    }

}
