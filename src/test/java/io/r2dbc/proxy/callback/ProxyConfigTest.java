/*
 * Copyright 2019-2020 the original author or authors.
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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.listener.ProxyMethodExecutionListener;
import io.r2dbc.proxy.listener.ProxyMethodExecutionListenerAdapter;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfigTest {

    @Test
    void setProxyFactoryFactory() {
        ProxyConfig proxyConfig = new ProxyConfig();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);

        when(proxyFactoryFactory.create(proxyConfig)).thenReturn(proxyFactory);

        proxyConfig.setProxyFactoryFactory(proxyFactoryFactory);

        assertThat(proxyConfig.getProxyFactory()).isSameAs(proxyFactory);

        assertThat(proxyConfig.getProxyFactory())
            .as("Second time calling getProxyFactory() should return same instance")
            .isSameAs(proxyFactory);
    }

    @Test
    @SuppressWarnings("deprecation")
    void builder() {
        ConnectionIdManager connectionIdManager = mock(ConnectionIdManager.class);
        Clock clock = mock(Clock.class);
        ProxyExecutionListener listener = mock(ProxyExecutionListener.class);
        io.r2dbc.proxy.listener.LifeCycleListener lifeCycleListener = mock(io.r2dbc.proxy.listener.LifeCycleListener.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = config -> proxyFactory;

        ProxyConfig.Builder builder = ProxyConfig.builder();
        builder.connectionIdManager(connectionIdManager)
            .clock(clock)
            .listener(listener)
            .listener(lifeCycleListener)
            .proxyFactoryFactory(proxyFactoryFactory);

        ProxyConfig proxyConfig = builder.build();

        assertThat(proxyConfig.getConnectionIdManager()).isSameAs(connectionIdManager);
        assertThat(proxyConfig.getClock()).isSameAs(clock);
        assertThat(proxyConfig.getListeners().getListeners())
            .hasSize(2)
            .contains(listener);
        assertThat(proxyConfig.getProxyFactory()).isSameAs(proxyFactory);
    }

    @Test
    void builderWithDefaultValues() {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();

        assertThat(proxyConfig.getConnectionIdManager()).isNotNull();
        assertThat(proxyConfig.getClock()).isNotNull();
        assertThat(proxyConfig.getListeners()).isNotNull();
        assertThat(proxyConfig.getProxyFactory()).isNotNull();
    }

    @Test
    void builderWithProxyMethodExecutionListener() {
        ProxyMethodExecutionListener methodListener = mock(ProxyMethodExecutionListener.class);

        ProxyConfig proxyConfig = ProxyConfig.builder().listener(methodListener).build();
        assertThat(proxyConfig.getListeners().getListeners())
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(ProxyMethodExecutionListenerAdapter.class, listener -> {
                assertThat(listener.getDelegate()).isSameAs(methodListener);
            });
    }

    @Test
    void addListener() {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();

        ProxyExecutionListener listener = mock(ProxyExecutionListener.class);
        proxyConfig.addListener(listener);

        assertThat(proxyConfig.getListeners().getListeners())
            .hasSize(1)
            .first()
            .isSameAs(listener);
    }

    @Test
    void addListenerWithProxyMethodExecutionListener() {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();

        ProxyMethodExecutionListener methodListener = mock(ProxyMethodExecutionListener.class);
        proxyConfig.addListener(methodListener);

        assertThat(proxyConfig.getListeners().getListeners())
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(ProxyMethodExecutionListenerAdapter.class, listener -> {
                assertThat(listener.getDelegate()).isSameAs(methodListener);
            });
    }

}
