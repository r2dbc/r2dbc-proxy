/*
 * Copyright 2022 the original author or authors.
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

package io.r2dbc.proxy.callback;

import reactor.util.context.ContextView;

import java.util.Map;
import java.util.stream.Stream;

/**
 * {@link ContextView} implementation that delegates to the given {@link ContextView} in constructor.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1.0
 */
public class DelegatingContextView implements ContextView {

    private final ContextView delegate;

    public DelegatingContextView(ContextView delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T get(Object key) {
        return this.delegate.get(key);
    }

    @Override
    public boolean hasKey(Object key) {
        return this.delegate.hasKey(key);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public Stream<Map.Entry<Object, Object>> stream() {
        return this.delegate.stream();
    }
}
