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

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class StatementCallbackHandlerTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Statement.class, "add");

    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Statement.class, "execute");

    private static Method BIND_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", int.class, Object.class);

    private static Method BIND_BY_ID_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", Object.class, Object.class);

    private static Method BIND_NULL_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", int.class, Class.class);

    private static Method BIND_NULL_BY_ID_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", Object.class, Class.class);

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    @Test
    void add() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        Statement proxyStatement = mock(Statement.class);

        when(originalStatement.add()).thenReturn(resultStatement);

        StatementCallbackHandler callback = new StatementCallbackHandler(originalStatement, "", connectionInfo, proxyConfig);

        Object result = callback.invoke(proxyStatement, ADD_METHOD, null);

        assertThat(result).isSameAs(proxyStatement);
    }

    @Test
    void bind() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        Statement proxyStatement = mock(Statement.class);

        when(originalStatement.bind(10, "foo")).thenReturn(resultStatement);

        StatementCallbackHandler callback = new StatementCallbackHandler(originalStatement, "", connectionInfo, proxyConfig);

        Object result = callback.invoke(proxyStatement, BIND_BY_INDEX_METHOD, new Object[]{10, "foo"});

        assertThat(result).isSameAs(proxyStatement);
    }

    @Test
    void executeOperationWithBindByIndex() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement statement = MockStatement.empty(); // make it return empty result
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        callback.invoke(statement, BIND_BY_INDEX_METHOD, new Object[]{1, 100});
        callback.invoke(statement, BIND_NULL_BY_INDEX_METHOD, new Object[]{2, String.class});
        callback.invoke(statement, ADD_METHOD, null);
        callback.invoke(statement, BIND_NULL_BY_INDEX_METHOD, new Object[]{1, int.class});
        callback.invoke(statement, BIND_BY_INDEX_METHOD, new Object[]{2, 200});
        Object result = callback.invoke(statement, EXECUTE_METHOD, null);


        StepVerifier.create((Publisher<?>) result)
            .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo).isNotNull();

        assertThat(afterQueryInfo.getBatchSize()).isEqualTo(0);
        assertThat(afterQueryInfo.getBindingsSize()).isEqualTo(2);
        assertThat(afterQueryInfo.getQueries())
            .hasSize(1)
            .extracting(QueryInfo::getQuery)
            .containsExactly(query);
        QueryInfo queryInfo = afterQueryInfo.getQueries().get(0);

        assertThat(queryInfo.getBindingsList()).hasSize(2);
        Bindings firstBindings = queryInfo.getBindingsList().get(0);
        Bindings secondBindings = queryInfo.getBindingsList().get(1);


        assertThat(firstBindings.getIndexBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly(1, 2);
        assertThat(firstBindings.getIdentifierBindings()).isEmpty();

        List<BoundValue> boundValues = firstBindings.getIndexBindings().stream()
            .map(Binding::getBoundValue)
            .collect(toList());

        // for "bind(1, 100)"
        assertThat(boundValues.get(0))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class)
            .extracting(BoundValue::getValue)
            .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(boundValues.get(1))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class);
        BoundValue nullBindValue = boundValues.get(1);
        assertThat(nullBindValue.getNullType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly(1, 2);
        assertThat(secondBindings.getIdentifierBindings()).isEmpty();

        boundValues = secondBindings.getIndexBindings().stream()
            .map(Binding::getBoundValue)
            .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(boundValues.get(0))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class);
        nullBindValue = boundValues.get(0);
        assertThat(nullBindValue.getNullType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(boundValues.get(1))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class)
            .extracting(BoundValue::getValue)
            .isEqualTo(200);

    }

    @Test
    void executeOperationWithBindByIdentifier() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement statement = MockStatement.empty(); // make it return empty result
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        callback.invoke(statement, BIND_BY_ID_METHOD, new Object[]{"$1", 100});
        callback.invoke(statement, BIND_NULL_BY_ID_METHOD, new Object[]{"$2", String.class});
        callback.invoke(statement, ADD_METHOD, null);
        callback.invoke(statement, BIND_NULL_BY_ID_METHOD, new Object[]{"$1", int.class});
        callback.invoke(statement, BIND_BY_ID_METHOD, new Object[]{"$2", 200});
        Object result = callback.invoke(statement, EXECUTE_METHOD, null);


        StepVerifier.create((Publisher<?>) result)
            .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo).isNotNull();

        assertThat(afterQueryInfo.getBatchSize()).isEqualTo(0);
        assertThat(afterQueryInfo.getBindingsSize()).isEqualTo(2);
        assertThat(afterQueryInfo.getQueries())
            .hasSize(1)
            .extracting(QueryInfo::getQuery)
            .containsExactly(query);
        QueryInfo queryInfo = afterQueryInfo.getQueries().get(0);

        assertThat(queryInfo.getBindingsList()).hasSize(2);
        Bindings firstBindings = queryInfo.getBindingsList().get(0);
        Bindings secondBindings = queryInfo.getBindingsList().get(1);


        assertThat(firstBindings.getIndexBindings()).isEmpty();
        assertThat(firstBindings.getIdentifierBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly("$1", "$2");

        List<BoundValue> boundValues = firstBindings.getIdentifierBindings().stream()
            .map(Binding::getBoundValue)
            .collect(toList());

        // for "bind(1, 100)"
        assertThat(boundValues.get(0))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class)
            .extracting(BoundValue::getValue)
            .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(boundValues.get(1))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class);
        BoundValue nullBindValue = boundValues.get(1);
        assertThat(nullBindValue.getNullType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings()).isEmpty();
        assertThat(secondBindings.getIdentifierBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly("$1", "$2");

        boundValues = secondBindings.getIdentifierBindings().stream()
            .map(Binding::getBoundValue)
            .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(boundValues.get(0))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class);
        nullBindValue = boundValues.get(0);
        assertThat(nullBindValue.getNullType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(boundValues.get(1))
            .isExactlyInstanceOf(BoundValue.DefaultBoundValue.class)
            .extracting(BoundValue::getValue)
            .isEqualTo(200);

    }

    @Test
    void unwrap() throws Throwable {
        Statement statement = MockStatement.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = new ProxyConfig();
        String query = "QUERY";

        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(statement);
    }

}
