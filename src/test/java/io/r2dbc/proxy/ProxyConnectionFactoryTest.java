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
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.test.MockMethodExecutionInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockConnectionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryTest {

    @Test
    void build() {
        ConnectionFactory connectionFactory = MockConnectionFactory.builder().build();

        ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
        ConnectionFactory result = builder.build();

        assertThat(result)
            .isNotSameAs(connectionFactory)
            .isInstanceOf(Wrapped.class);

        Object unwrapped = ((Wrapped) result).unwrap();
        assertThat(unwrapped).isSameAs(connectionFactory);
    }

    @Test
    void builder() {

        ConnectionFactory connectionFactory = MockConnectionFactory.builder().build();
        ProxyConfig mockProxyConfig = mock(ProxyConfig.class);

        AtomicBoolean onBeforeMethod = new AtomicBoolean();
        AtomicBoolean onAfterMethod = new AtomicBoolean();
        AtomicBoolean onBeforeQuery = new AtomicBoolean();
        AtomicBoolean onAfterQuery = new AtomicBoolean();
        AtomicBoolean onEachQueryResult = new AtomicBoolean();

        // create a builder and register callbacks
        ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory, mockProxyConfig);
        builder.onBeforeMethod(execInfo -> onBeforeMethod.set(true));
        builder.onAfterMethod(execInfo -> onAfterMethod.set(true));
        builder.onBeforeQuery(execInfo -> onBeforeQuery.set(true));
        builder.onAfterQuery(execInfo -> onAfterQuery.set(true));
        builder.onEachQueryResult(execInfo -> onEachQueryResult.set(true));

        // create an invocation captor for "proxyConfig.addListener()"
        ArgumentCaptor<ProxyExecutionListener> addListenerCaptor = ArgumentCaptor.forClass(ProxyExecutionListener.class);

        verify(mockProxyConfig, times(5)).addListener(addListenerCaptor.capture());

        // at this point, none of the listener methods are invoked yet
        assertThat(onBeforeMethod).isFalse();
        assertThat(onAfterMethod).isFalse();
        assertThat(onBeforeQuery).isFalse();
        assertThat(onAfterQuery).isFalse();
        assertThat(onEachQueryResult).isFalse();

        List<ProxyExecutionListener> listeners = addListenerCaptor.getAllValues();

        // invoke first listener and verify callback is called
        listeners.get(0).beforeMethod(MockMethodExecutionInfo.empty());
        assertThat(onBeforeMethod).isTrue();

        // invoke second listener and verify callback is called
        listeners.get(1).afterMethod(MockMethodExecutionInfo.empty());
        assertThat(onAfterMethod).isTrue();

        // invoke third listener and verify callback is called
        listeners.get(2).beforeQuery(MockQueryExecutionInfo.empty());
        assertThat(onBeforeQuery).isTrue();

        // invoke fourth listener and verify callback is called
        listeners.get(3).afterQuery(MockQueryExecutionInfo.empty());
        assertThat(onAfterQuery).isTrue();

        // invoke fifth listener and verify callback is called
        listeners.get(4).eachQueryResult(MockQueryExecutionInfo.empty());
        assertThat(onEachQueryResult).isTrue();
    }

}
