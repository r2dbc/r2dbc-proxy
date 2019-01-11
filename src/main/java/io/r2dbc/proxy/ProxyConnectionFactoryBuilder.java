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
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

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

    /**
     * Create a {@link ProxyConnectionFactoryBuilder}.
     *
     * @param delegate actual {@link ConnectionFactory}
     */
    public ProxyConnectionFactoryBuilder(ConnectionFactory delegate) {
        this.delegate = Assert.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Create a {@link ProxyConnectionFactoryBuilder}.
     *
     * @param delegate actual {@link ConnectionFactory}
     * @return builder
     */
    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate) {
        Assert.requireNonNull(delegate, "delegate must not be null");
        return new ProxyConnectionFactoryBuilder(delegate);
    }

    /**
     * Create a {@link ProxyConnectionFactoryBuilder}.
     *
     * @param delegate    actual {@link ConnectionFactory}
     * @param proxyConfig {@link ProxyConfig} to use
     * @return builder
     */
    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate, ProxyConfig proxyConfig) {
        Assert.requireNonNull(delegate, "delegate must not be null");
        Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");
        return create(delegate).proxyConfig(proxyConfig);
    }

    /**
     * Build a proxy {@link ConnectionFactory}.
     *
     * @return a {@link ConnectionFactory}
     */
    public ConnectionFactory build() {
        return this.proxyConfig.getProxyFactory().wrapConnectionFactory(this.delegate);
    }


    /**
     * Set a {@link ProxyConfig} to use.
     *
     * @param proxyConfig proxy config
     * @return builder
     */
    public ProxyConnectionFactoryBuilder proxyConfig(ProxyConfig proxyConfig) {
        Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");
        this.proxyConfig = proxyConfig;
        return this;
    }

    /**
     * Register a callback consumer for before method execution
     *
     * @param consumer a consumer for before method execution
     * @return builder
     */
    public ProxyConnectionFactoryBuilder onBeforeMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    /**
     * Register a callback consumer for after method execution.
     *
     * @param consumer a consumer for after method execution
     * @return builder
     */
    public ProxyConnectionFactoryBuilder onAfterMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    /**
     * Register a callback consumer for before query execution.
     *
     * @param consumer a consumer for before query execution
     * @return builder
     */
    public ProxyConnectionFactoryBuilder onBeforeQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void beforeQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    /**
     * Register a callback consumer for after query execution.
     *
     * @param consumer a consumer for after query execution
     * @return builder
     */
    public ProxyConnectionFactoryBuilder onAfterQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void afterQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    /**
     * Register a callback consumer for processing each query result.
     *
     * @param consumer a consumer for each query result
     * @return builder
     */
    public ProxyConnectionFactoryBuilder onEachQueryResult(Consumer<Mono<QueryExecutionInfo>> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");
        this.proxyConfig.addListener(new ProxyExecutionListener() {

            @Override
            public void eachQueryResult(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    /**
     * Register a {@link ProxyExecutionListener}.
     *
     * @param listener a listener to register
     * @return builder
     */
    public ProxyConnectionFactoryBuilder listener(ProxyExecutionListener listener) {
        Assert.requireNonNull(listener, "listener must not be null");
        this.proxyConfig.addListener(listener);
        return this;
    }

    /**
     * Register a {@link LifeCycleListener}.
     *
     * @param lifeCycleListener a listener to register
     * @return builder
     */
    public ProxyConnectionFactoryBuilder listener(LifeCycleListener lifeCycleListener) {
        Assert.requireNonNull(lifeCycleListener, "lifeCycleListener must not be null");
        this.listener(LifeCycleExecutionListener.of(lifeCycleListener));
        return this;
    }

}
