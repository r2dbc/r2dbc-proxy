/*
 * Copyright 2018 the original author or authors.
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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class BatchCallbackHandlerTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Batch.class, "add", String.class);

    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Batch.class, "execute");

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    @Test
    @SuppressWarnings("unchecked")
    void batchOperation() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Batch batch = mock(Batch.class);
        BatchCallbackHandler callback = new BatchCallbackHandler(batch, connectionInfo, proxyConfig);

        // mock batch execution
        when(batch.execute()).thenReturn(Flux.empty());

        String query1 = "QUERY-1";
        String query2 = "QUERY-2";

        callback.invoke(batch, ADD_METHOD, new String[]{query1});
        callback.invoke(batch, ADD_METHOD, new String[]{query2});

        Object result = callback.invoke(batch, EXECUTE_METHOD, new String[]{});


        StepVerifier.create((Publisher<? extends Result>) result)
            .verifyComplete();

        QueryExecutionInfo beforeQueryInfo = testListener.getBeforeQueryExecutionInfo();
        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(beforeQueryInfo).isNotNull();
        assertThat(beforeQueryInfo.getBatchSize()).isEqualTo(2);
        assertThat(beforeQueryInfo.getBindingsSize()).isEqualTo(0);
        assertThat(beforeQueryInfo.isSuccess()).isTrue();
        assertThat(beforeQueryInfo.getType()).isEqualTo(ExecutionType.BATCH);
        assertThat(beforeQueryInfo.getConnectionInfo()).isSameAs(connectionInfo);
        assertThat(beforeQueryInfo.getQueries())
            .extracting(QueryInfo::getQuery)
            .containsExactly("QUERY-1", "QUERY-2");

        assertThat(afterQueryInfo).isNotNull();
        assertThat(afterQueryInfo.getBatchSize()).isEqualTo(2);
        assertThat(afterQueryInfo.getBindingsSize()).isEqualTo(0);
        assertThat(afterQueryInfo.isSuccess()).isTrue();
        assertThat(afterQueryInfo.getType()).isEqualTo(ExecutionType.BATCH);
        assertThat(afterQueryInfo.getConnectionInfo()).isSameAs(connectionInfo);
        assertThat(afterQueryInfo.getQueries())
            .extracting(QueryInfo::getQuery)
            .containsExactly("QUERY-1", "QUERY-2");

    }

    @Test
    void unwrap() throws Throwable {
        Batch batch = mock(Batch.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = new ProxyConfig();

        BatchCallbackHandler callback = new BatchCallbackHandler(batch, connectionInfo, proxyConfig);

        Object result = callback.invoke(batch, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(batch);
    }

    @Test
    void add() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Batch originalBatch = mock(Batch.class);
        Batch proxyBatch = mock(Batch.class);
        Batch resultBatch = mock(Batch.class);
        BatchCallbackHandler callback = new BatchCallbackHandler(originalBatch, connectionInfo, proxyConfig);

        // mock batch execution
        String query1 = "QUERY-1";
        when(originalBatch.add(query1)).thenReturn(resultBatch);

        Object result = callback.invoke(proxyBatch, ADD_METHOD, new String[]{query1});

        assertThat(result).isSameAs(proxyBatch);
    }

}
