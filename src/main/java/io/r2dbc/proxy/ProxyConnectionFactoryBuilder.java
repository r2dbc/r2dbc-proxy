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

package io.r2dbc.proxy;

import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.LifeCycleExecutionListener;
import io.r2dbc.proxy.listener.LifeCycleListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Entry point to create a proxy for {@link ConnectionFactory}.
 *
 * The returned {@link ConnectionFactory} is a proxy. Registered listeners and configuration will
 * be used throughout the operations of the proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryBuilder {

    private ConnectionFactory delegate;

    private ProxyConfig proxyConfig = new ProxyConfig(); // default

    public ProxyConnectionFactoryBuilder(ConnectionFactory delegate) {
        this.delegate = delegate;
    }

    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate) {
        Objects.requireNonNull(delegate, "ConnectionFactory to delegate is required");
        return new ProxyConnectionFactoryBuilder(delegate);
    }

    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate, ProxyConfig proxyConfig) {
        return create(delegate).proxyConfig(proxyConfig);
    }

    public ConnectionFactory build() {
        return this.proxyConfig.getProxyFactory().createProxyConnectionFactory(this.delegate);
    }


    public ProxyConnectionFactoryBuilder proxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
        return this;
    }

    public ProxyConnectionFactoryBuilder onBeforeMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onAfterMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onBeforeQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void beforeQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onAfterQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void afterQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onEachQueryResult(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void eachQueryResult(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder listener(ProxyExecutionListener listener) {
        this.proxyConfig.addListener(listener);
        return this;
    }

    public ProxyConnectionFactoryBuilder listener(LifeCycleListener lifeCycleListener) {
        this.listener(LifeCycleExecutionListener.of(lifeCycleListener));
        return this;
    }

}
