/*
 * Copyright 2018-2020 the original author or authors.
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

import io.r2dbc.proxy.listener.BindParameterConverter;
import io.r2dbc.proxy.listener.CompositeProxyExecutionListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.listener.ResultRowConverter;
import io.r2dbc.proxy.util.Assert;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration object for proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfig {

    private static final ConnectionIdManager DEFAULT_CONNECTION_ID_MANAGER = ConnectionIdManager.create();

    private static final ProxyFactoryFactory DEFAULT_PROXY_FACTORY_FACTORY = new JdkProxyFactoryFactory();

    private static final BindParameterConverter DEFAULT_BIND_PARAMETER_CONVERTER = BindParameterConverter.create();

    private static final ResultRowConverter DEFAULT_RESULT_ROW_CONVERTER = ResultRowConverter.create();

    private static final Clock DEFAULT_CLOCK = Clock.systemUTC();

    private final CompositeProxyExecutionListener listeners = new CompositeProxyExecutionListener();

    private ConnectionIdManager connectionIdManager = DEFAULT_CONNECTION_ID_MANAGER;

    private ProxyFactory proxyFactory = DEFAULT_PROXY_FACTORY_FACTORY.create(this);

    private Clock clock = DEFAULT_CLOCK;

    private BindParameterConverter bindParameterConverter = DEFAULT_BIND_PARAMETER_CONVERTER;

    private ResultRowConverter resultRowConverter = DEFAULT_RESULT_ROW_CONVERTER;

    /**
     * Create a new {@link Builder}.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public ProxyConfig() {
    }

    private ProxyConfig(Builder builder) {
        this.connectionIdManager = builder.connectionIdManager;
        this.clock = builder.clock;
        this.listeners.addAll(builder.listeners);
        this.bindParameterConverter = builder.bindParameterConverter;
        this.resultRowConverter = builder.resultRowConverter;
        this.proxyFactory = builder.proxyFactoryFactory.create(this);
    }

    /**
     * Set {@link ProxyFactoryFactory}.
     *
     * When {@link ProxyFactoryFactory} is set, {@link ProxyFactoryFactory#create(ProxyConfig)} method
     * is called once to generate {@link ProxyFactory}. The generated {@link ProxyFactory} instance is
     * always returned by {@link #getProxyFactory()} unless this method is called to set a new
     * {@link ProxyFactoryFactory}.
     *
     * @param proxyFactoryFactory factory for {@link ProxyFactory}
     * @throws IllegalArgumentException if {@code proxyFactoryFactory} is {@code null}
     */
    public void setProxyFactoryFactory(ProxyFactoryFactory proxyFactoryFactory) {
        Assert.requireNonNull(proxyFactoryFactory, "proxyFactoryFactory must not be null");

        this.proxyFactory = proxyFactoryFactory.create(this);
    }

    /**
     * Get {@link ProxyFactory} which is generated from the specified {@link ProxyFactoryFactory}.
     *
     * Always same instance of {@link ProxyFactory} is returned.
     *
     * @return proxy factory
     */
    public ProxyFactory getProxyFactory() {
        return this.proxyFactory;
    }

    /**
     * Returns {@link CompositeProxyExecutionListener} that contains registered {@link ProxyExecutionListener}.
     *
     * @return composite proxy execution listener
     */
    public CompositeProxyExecutionListener getListeners() {
        return this.listeners;
    }

    /**
     * Register {@link ProxyExecutionListener}.
     *
     * @param listener a listener to register
     * @throws IllegalArgumentException if {@code proxyFactoryFactory} is {@code null}
     */
    public void addListener(ProxyExecutionListener listener) {
        Assert.requireNonNull(listener, "listener must not be null");

        this.listeners.add(listener);
    }

    /**
     * Get {@link ConnectionIdManager}.
     *
     * @return connection id manager
     */
    public ConnectionIdManager getConnectionIdManager() {
        return this.connectionIdManager;
    }

    /**
     * Set {@link ConnectionIdManager}.
     *
     * @param connectionIdManager connection id manager
     * @throws IllegalArgumentException if {@code connectionIdManager} is {@code null}
     */
    public void setConnectionIdManager(ConnectionIdManager connectionIdManager) {
        this.connectionIdManager = Assert.requireNonNull(connectionIdManager, "connectionIdManager must not be null");
    }

    /**
     * Get {@link Clock}.
     *
     * @return clock to use
     */
    public Clock getClock() {
        return this.clock;
    }

    /**
     * Set {@link Clock} to use to calculate the elapsed time.
     *
     * @param clock clock to use
     * @throws IllegalArgumentException if {@code clock} is {@code null}
     * @see CallbackHandlerSupport
     */
    public void setClock(Clock clock) {
        this.clock = Assert.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Get {@link BindParameterConverter}.
     *
     * @return bindParameterConverter to use
     */
    public BindParameterConverter getBindParameterConverter() {
        return this.bindParameterConverter;
    }

    /**
     * Set {@link BindParameterConverter}.
     *
     * @param bindParameterConverter bind parameter converter
     * @throws IllegalArgumentException if {@code bindParameterConverter} is {@code null}
     */
    public void setBindParameterConverter(BindParameterConverter bindParameterConverter) {
        this.bindParameterConverter = Assert.requireNonNull(bindParameterConverter, "bindParameterConverter must not be null");
    }

    /**
     * Get {@link ResultRowConverter}.
     *
     * @return resultRowConverter to use
     * @since 0.9.0
     */
    public ResultRowConverter getResultRowConverter() {
        return this.resultRowConverter;
    }

    /**
     * Set {@link ResultRowConverter}.
     *
     * @param resultRowConverter the result row converter
     * @throws IllegalArgumentException if {@code resultRowConverter} is {@code null}
     * @since 0.9.0
     */
    public void setResultRowConverter(ResultRowConverter resultRowConverter) {
        this.resultRowConverter = Assert.requireNonNull(resultRowConverter, "resultRowConverter must not be null");
    }

    /**
     * Builder to create a {@link ProxyConfig}.
     *
     * For attributes that are not specified, default values will be used.
     */
    public static final class Builder {

        private List<ProxyExecutionListener> listeners = new ArrayList<>();

        private ConnectionIdManager connectionIdManager = DEFAULT_CONNECTION_ID_MANAGER;

        private ProxyFactoryFactory proxyFactoryFactory = DEFAULT_PROXY_FACTORY_FACTORY;

        private BindParameterConverter bindParameterConverter = DEFAULT_BIND_PARAMETER_CONVERTER;

        private ResultRowConverter resultRowConverter = DEFAULT_RESULT_ROW_CONVERTER;

        private Clock clock = DEFAULT_CLOCK;

        /**
         * Add a {@link ProxyExecutionListener}.
         *
         * @param listener a listener to add
         * @return builder
         * @throws IllegalArgumentException if {@code listener} is {@code null}
         */
        public Builder listener(ProxyExecutionListener listener) {
            this.listeners.add(Assert.requireNonNull(listener, "listener must not be null"));
            return this;
        }

        /**
         * Set {@link ConnectionIdManager}.
         *
         * @param connectionIdManager connectionIdManager to be used
         * @return builder
         * @throws IllegalArgumentException if {@code connectionIdManager} is {@code null}
         */
        public Builder connectionIdManager(ConnectionIdManager connectionIdManager) {
            this.connectionIdManager = Assert.requireNonNull(connectionIdManager, "connectionIdManager must not be null");
            return this;
        }

        /**
         * Set {@link ProxyFactoryFactory}.
         *
         * @param proxyFactoryFactory proxyFactoryFactory to be used
         * @return builder
         * @throws IllegalArgumentException if {@code proxyFactoryFactory} is {@code null}
         */
        public Builder proxyFactoryFactory(ProxyFactoryFactory proxyFactoryFactory) {
            this.proxyFactoryFactory = Assert.requireNonNull(proxyFactoryFactory, "proxyFactoryFactory must not be null");
            return this;
        }

        /**
         * Set {@link Clock}.
         *
         * @param clock clock to be used
         * @return builder
         * @throws IllegalArgumentException if {@code clock} is {@code null}
         */
        public Builder clock(Clock clock) {
            this.clock = Assert.requireNonNull(clock, "clock must not be null");
            return this;
        }

        /**
         * Set {@link BindParameterConverter}.
         *
         * @param bindParameterConverter bindParameterConverter to be used
         * @return builder
         * @throws IllegalArgumentException if {@code bindParameterConverter} is {@code null}
         */
        public Builder bindParameterConverter(BindParameterConverter bindParameterConverter) {
            this.bindParameterConverter = Assert.requireNonNull(bindParameterConverter, "bindParameterConverter must not be null");
            return this;
        }

        /**
         * Set {@link ResultRowConverter}.
         *
         * @param resultRowConverter resultRowConverter to be used
         * @return builder
         * @throws IllegalArgumentException if {@code resultRowConverter} is {@code null}
         * @since 0.9.0
         */
        public Builder resultRowConverter(ResultRowConverter resultRowConverter) {
            this.resultRowConverter = Assert.requireNonNull(resultRowConverter, "resultRowConverter must not be null");
            return this;
        }

        /**
         * Build a {@link ProxyConfig}.
         *
         * @return a {@link ProxyConfig}
         */
        public ProxyConfig build() {
            return new ProxyConfig(this);
        }

    }

}
