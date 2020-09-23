/*
 * Copyright 2020 the original author or authors.
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

import io.r2dbc.proxy.core.MethodExecutionInfo;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.util.annotation.Nullable;

import java.util.function.Consumer;

/**
 * A flux operator that calls before/after method callbacks.
 *
 * @author Tadaya Tsuyukubo
 * @see MethodInvocationSubscriber
 */
class FluxMethodInvocation extends FluxOperator<Object, Object> {

    private final MutableMethodExecutionInfo executionInfo;

    private final ProxyConfig proxyConfig;

    @Nullable
    private final Consumer<MethodExecutionInfo> onComplete;

    public FluxMethodInvocation(Flux<?> source, MutableMethodExecutionInfo executionInfo, ProxyConfig proxyConfig, @Nullable Consumer<MethodExecutionInfo> onComplete) {
        super(source);
        this.executionInfo = executionInfo;
        this.proxyConfig = proxyConfig;
        this.onComplete = onComplete;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Object> actual) {
        this.source.subscribe(new MethodInvocationSubscriber(actual, this.executionInfo, this.proxyConfig, this.onComplete));
    }

}
