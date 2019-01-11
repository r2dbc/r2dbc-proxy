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

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.BindingValue;
import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
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

        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement<?> statement = mock(Statement.class);
        Statement<?> mockResult = mock(Statement.class);

        doReturn(mockResult).when(statement).add();

        StatementCallbackHandler callback = new StatementCallbackHandler(statement, "", connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, ADD_METHOD, null);

        assertThat(result).isSameAs(mockResult);
    }

    @Test
    void executeOperationWithBindByIndex() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement<?> statement = mock(Statement.class);
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        when(statement.execute()).thenReturn(Flux.empty());

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

        List<BindingValue> bindingValues = firstBindings.getIndexBindings().stream()
            .map(Binding::getBindingValue)
            .collect(toList());

        // for "bind(1, 100)"
        assertThat(bindingValues.get(0))
            .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
            .extracting(BindingValue::getValue)
            .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(bindingValues.get(1))
            .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        BindingValue.NullBindingValue nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(1);
        assertThat(nullBindValue.getType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly(1, 2);
        assertThat(secondBindings.getIdentifierBindings()).isEmpty();

        bindingValues = secondBindings.getIndexBindings().stream()
            .map(Binding::getBindingValue)
            .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(bindingValues.get(0))
            .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(0);
        assertThat(nullBindValue.getType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(bindingValues.get(1))
            .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
            .extracting(BindingValue::getValue)
            .isEqualTo(200);

    }

    @Test
    void executeOperationWithBindByIdentifier() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement<?> statement = mock(Statement.class);
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        when(statement.execute()).thenReturn(Flux.empty());

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

        List<BindingValue> bindingValues = firstBindings.getIdentifierBindings().stream()
            .map(Binding::getBindingValue)
            .collect(toList());

        // for "bind(1, 100)"
        assertThat(bindingValues.get(0))
            .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
            .extracting(BindingValue::getValue)
            .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(bindingValues.get(1))
            .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        BindingValue.NullBindingValue nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(1);
        assertThat(nullBindValue.getType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings()).isEmpty();
        assertThat(secondBindings.getIdentifierBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly("$1", "$2");

        bindingValues = secondBindings.getIdentifierBindings().stream()
            .map(Binding::getBindingValue)
            .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(bindingValues.get(0))
            .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(0);
        assertThat(nullBindValue.getType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(bindingValues.get(1))
            .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
            .extracting(BindingValue::getValue)
            .isEqualTo(200);

    }

    @Test
    void unwrap() throws Throwable {
        Statement<?> statement = mock(Statement.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        String query = "QUERY";

        StatementCallbackHandler callback = new StatementCallbackHandler(statement, query, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(statement);
    }

}
