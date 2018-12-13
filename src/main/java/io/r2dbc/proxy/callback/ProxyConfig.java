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

/**
 * Central configuration object for proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfig {

    private CompositeProxyExecutionListener listeners = new CompositeProxyExecutionListener();

    private ConnectionIdManager connectionIdManager = new DefaultConnectionIdManager();

    private ProxyFactory proxyFactory = new JdkProxyFactory();

    public ProxyConfig() {
        this.proxyFactory.setProxyConfig(this);
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
        this.proxyFactory.setProxyConfig(this);
    }

    public CompositeProxyExecutionListener getListeners() {
        return this.listeners;
    }

    public void addListener(ProxyExecutionListener listener) {
        this.listeners.add(listener);
    }

    public ConnectionIdManager getConnectionIdManager() {
        return connectionIdManager;
    }

    public void setConnectionIdManager(ConnectionIdManager connectionIdManager) {
        this.connectionIdManager = connectionIdManager;
    }
}
