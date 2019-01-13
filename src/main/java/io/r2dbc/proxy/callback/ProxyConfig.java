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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.listener.CompositeProxyExecutionListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.util.Assert;

/**
 * Central configuration object for proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfig {

    private CompositeProxyExecutionListener listeners = new CompositeProxyExecutionListener();

    private ConnectionIdManager connectionIdManager = ConnectionIdManager.create();

    private ProxyFactory proxyFactory = new JdkProxyFactoryFactory().create(this);

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
     * @param listener listner to register
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
        Assert.requireNonNull(connectionIdManager, "connectionIdManager must not be null");

        this.connectionIdManager = connectionIdManager;
    }
}
