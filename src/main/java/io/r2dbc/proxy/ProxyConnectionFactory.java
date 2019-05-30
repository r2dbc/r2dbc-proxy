/*
 * Copyright 2019 the original author or authors.
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
 *
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

import java.util.function.Consumer;

/**
 * Entry point to create a proxy for the given {@link ConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class ProxyConnectionFactory {

    private ProxyConnectionFactory() {
    }

    /**
     * Create a new {@link Builder}.
     *
     * @param connectionFactory actual {@link ConnectionFactory}
     * @return builder
     * @throws IllegalArgumentException if {@code connectionFactory} is {@code null}
     */
    public static Builder builder(ConnectionFactory connectionFactory) {
        Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");

        return new Builder(connectionFactory);
    }

    /**
     * Create a new {@link Builder}.
     *
     * @param connectionFactory actual {@link ConnectionFactory}
     * @param proxyConfig       {@link ProxyConfig} to use
     * @return builder
     * @throws IllegalArgumentException if {@code connectionFactory} is {@code null}
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     */
    public static Builder builder(ConnectionFactory connectionFactory, ProxyConfig proxyConfig) {
        Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");
        Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");

        return builder(connectionFactory).proxyConfig(proxyConfig);
    }


    /**
     * Builder to create a proxy {@link ConnectionFactory}.
     *
     * The returned {@link ConnectionFactory} is a proxy.
     * Registered listeners and configuration will be used throughout the operations of the proxy.
     *
     * <i>This class is not threadsafe.</i>
     */
    public static final class Builder {

        private final ConnectionFactory connectionFactory;  // actual ConnectionFactory

        private ProxyConfig proxyConfig = new ProxyConfig(); // default

        /**
         * Constructor.
         *
         * @param connectionFactory actual {@link ConnectionFactory}
         * @throws IllegalArgumentException if {@code connectionFactory} is {@code null}
         */
        private Builder(ConnectionFactory connectionFactory) {
            this.connectionFactory = Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");
        }

        /**
         * Build a proxy {@link ConnectionFactory}.
         *
         * @return a proxy {@link ConnectionFactory}
         */
        public ConnectionFactory build() {
            return this.proxyConfig.getProxyFactory().wrapConnectionFactory(this.connectionFactory);
        }

        /**
         * Set a {@link ProxyConfig} to use.
         *
         * @param proxyConfig proxy config
         * @return builder
         * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
         */
        public Builder proxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");

            return this;
        }

        /**
         * Register a callback consumer for before method execution
         *
         * @param consumer a consumer for before method execution
         * @return builder
         * @throws IllegalArgumentException if {@code consumer} is {@code null}
         */
        public Builder onBeforeMethod(Consumer<MethodExecutionInfo> consumer) {
            Assert.requireNonNull(consumer, "consumer must not be null");

            this.proxyConfig.addListener(new ProxyExecutionListener() {

                @Override
                public void beforeMethod(MethodExecutionInfo executionInfo) {
                    consumer.accept(executionInfo);
                }
            });
            return this;
        }

        /**
         * Register a callback consumer for after method execution.
         *
         * @param consumer a consumer for after method execution
         * @return builder
         * @throws IllegalArgumentException if {@code consumer} is {@code null}
         */
        public Builder onAfterMethod(Consumer<MethodExecutionInfo> consumer) {
            Assert.requireNonNull(consumer, "consumer must not be null");

            this.proxyConfig.addListener(new ProxyExecutionListener() {

                @Override
                public void afterMethod(MethodExecutionInfo executionInfo) {
                    consumer.accept(executionInfo);
                }
            });
            return this;
        }

        /**
         * Register a callback consumer for before query execution.
         *
         * @param consumer a consumer for before query execution
         * @return builder
         * @throws IllegalArgumentException if {@code consumer} is {@code null}
         */
        public Builder onBeforeQuery(Consumer<QueryExecutionInfo> consumer) {
            Assert.requireNonNull(consumer, "consumer must not be null");

            this.proxyConfig.addListener(new ProxyExecutionListener() {

                @Override
                public void beforeQuery(QueryExecutionInfo executionInfo) {
                    consumer.accept(executionInfo);
                }
            });
            return this;
        }

        /**
         * Register a callback consumer for after query execution.
         *
         * @param consumer a consumer for after query execution
         * @return builder
         * @throws IllegalArgumentException if {@code consumer} is {@code null}
         */
        public Builder onAfterQuery(Consumer<QueryExecutionInfo> consumer) {
            Assert.requireNonNull(consumer, "consumer must not be null");

            this.proxyConfig.addListener(new ProxyExecutionListener() {

                @Override
                public void afterQuery(QueryExecutionInfo executionInfo) {
                    consumer.accept(executionInfo);
                }
            });
            return this;
        }

        /**
         * Register a callback consumer for processing each query result.
         *
         * @param consumer a consumer for each query result
         * @return builder
         * @throws IllegalArgumentException if {@code consumer} is {@code null}
         */
        public Builder onEachQueryResult(Consumer<QueryExecutionInfo> consumer) {
            Assert.requireNonNull(consumer, "consumer must not be null");

            this.proxyConfig.addListener(new ProxyExecutionListener() {

                @Override
                public void eachQueryResult(QueryExecutionInfo executionInfo) {
                    consumer.accept(executionInfo);
                }
            });
            return this;
        }

        /**
         * Register a {@link ProxyExecutionListener}.
         *
         * @param listener a listener to register
         * @return builder
         * @throws IllegalArgumentException if {@code listener} is {@code null}
         */
        public Builder listener(ProxyExecutionListener listener) {
            Assert.requireNonNull(listener, "listener must not be null");

            this.proxyConfig.addListener(listener);
            return this;
        }

        /**
         * Register a {@link LifeCycleListener}.
         *
         * @param lifeCycleListener a listener to register
         * @return builder
         * @throws IllegalArgumentException if {@code lifeCycleListener} is {@code null}
         */
        public Builder listener(LifeCycleListener lifeCycleListener) {
            Assert.requireNonNull(lifeCycleListener, "lifeCycleListener must not be null");

            this.listener(LifeCycleExecutionListener.of(lifeCycleListener));
            return this;
        }

    }
}
