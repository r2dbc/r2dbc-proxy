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
 */

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.callback.BatchCallbackHandler;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.ValueStore;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyExecutionListenerTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Batch.class, "add", String.class);
    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Batch.class, "execute");

    @Test
    void valueStoreWithMethodExecution() throws Throwable {

        AtomicReference<ValueStore> beforeMethodValueStoreHolder = new AtomicReference<>();
        AtomicReference<ValueStore> afterMethodValueStoreHolder = new AtomicReference<>();

        ProxyExecutionListener listener = new ProxyExecutionListener() {
            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                executionInfo.getValueStore().put("foo", "FOO");
                beforeMethodValueStoreHolder.set(executionInfo.getValueStore());
            }

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                assertThat(executionInfo.getValueStore().get("foo")).isEqualTo("FOO");
                afterMethodValueStoreHolder.set(executionInfo.getValueStore());
            }
        };

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        Batch originalBatch = mock(Batch.class);
        Batch proxyBatch = mock(Batch.class);
        Batch resultBatch = mock(Batch.class);
        BatchCallbackHandler callback = new BatchCallbackHandler(originalBatch, connectionInfo, proxyConfig);

        // mock batch execution
        when(originalBatch.add("MY-QUERY")).thenReturn(resultBatch);

        Object result = callback.invoke(proxyBatch, ADD_METHOD, new String[]{"MY-QUERY"});

        assertThat(result).isSameAs(proxyBatch);

        assertThat(beforeMethodValueStoreHolder).doesNotHaveValue(null);
        assertThat(afterMethodValueStoreHolder).doesNotHaveValue(null);
        assertThat(beforeMethodValueStoreHolder.get()).isEqualTo(afterMethodValueStoreHolder.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void valueStoreWithQueryExecution() throws Throwable {

        AtomicReference<ValueStore> beforeQueryValueStoreHolder = new AtomicReference<>();
        AtomicReference<ValueStore> afterQueryValueStoreHolder = new AtomicReference<>();

        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void beforeQuery(QueryExecutionInfo execInfo) {
                execInfo.getValueStore().put("foo", "FOO");
                beforeQueryValueStoreHolder.set(execInfo.getValueStore());
            }

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                assertThat(execInfo.getValueStore().get("foo")).isEqualTo("FOO");
                afterQueryValueStoreHolder.set(execInfo.getValueStore());
            }

        };

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        Batch batch = mock(Batch.class);
        when(batch.execute()).thenReturn(Flux.empty());  // mock batch execution

        BatchCallbackHandler callback = new BatchCallbackHandler(batch, connectionInfo, proxyConfig);
        Object result = callback.invoke(batch, EXECUTE_METHOD, new String[]{});

        StepVerifier.create((Publisher<? extends Result>) result)
            .verifyComplete();

        assertThat(beforeQueryValueStoreHolder).doesNotHaveValue(null);
        assertThat(afterQueryValueStoreHolder).doesNotHaveValue(null);
        assertThat(beforeQueryValueStoreHolder.get()).isEqualTo(afterQueryValueStoreHolder.get());
    }

}
