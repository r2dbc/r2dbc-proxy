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

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;

/**
 * Special Mono operator for {@link ConnectionFactory#create()} to invoke before/after
 * method callbacks.
 *
 * @author Tadaya Tsuyukubo
 */
class MonoMethodInvocationConnectionFactoryCreate extends MonoOperator<Object, Object> {

    private final MutableMethodExecutionInfo executionInfo;

    private final ProxyConfig proxyConfig;

    public MonoMethodInvocationConnectionFactoryCreate(Mono<?> source, MutableMethodExecutionInfo executionInfo, ProxyConfig proxyConfig) {
        super(source);
        this.executionInfo = executionInfo;
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Object> actual) {
        this.source.subscribe(new MonoMethodInvocationConnectionFactoryCreateSubscriber(actual, this.executionInfo, this.proxyConfig));
    }

    static class MonoMethodInvocationConnectionFactoryCreateSubscriber extends MethodInvocationSubscriber {

        public MonoMethodInvocationConnectionFactoryCreateSubscriber(CoreSubscriber<Object> delegate, MutableMethodExecutionInfo executionInfo, ProxyConfig proxyConfig) {
            super(delegate, executionInfo, proxyConfig, null);
        }

        @Override
        public void onComplete() {
            // "doOnSuccess()" chained to this operator calls "afterMethod()" callback.
            // Therefore, do not call "afterMethod" on onComplete().
            // see "ConnectionFactoryCallbackHandler" and how it handles "create" method.
            this.delegate.onComplete();
        }

    }

}
