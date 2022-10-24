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

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockStatementInfo;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link StatementCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
public class StatementCallbackHandlerTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Statement.class, "add");

    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Statement.class, "execute");

    private static Method BIND_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", int.class, Object.class);

    private static Method BIND_BY_NAME_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", String.class, Object.class);

    private static Method BIND_NULL_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", int.class, Class.class);

    private static Method BIND_NULL_BY_NAME_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", String.class, Class.class);

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");

    @Test
    void add() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.empty();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        Statement proxyStatement = mock(Statement.class);

        when(originalStatement.add()).thenReturn(resultStatement);

        StatementCallbackHandler callback = new StatementCallbackHandler(originalStatement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(proxyStatement, ADD_METHOD, null);

        assertThat(result).isSameAs(proxyStatement);
    }

    @Test
    void bind() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.empty();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        Statement proxyStatement = mock(Statement.class);

        when(originalStatement.bind(10, "foo")).thenReturn(resultStatement);

        StatementCallbackHandler callback = new StatementCallbackHandler(originalStatement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(proxyStatement, BIND_BY_INDEX_METHOD, new Object[]{10, "foo"});

        assertThat(result).isSameAs(proxyStatement);
    }

    @Test
    void executeOperationWithBindByIndex() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery(query).build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement statement = MockStatement.empty(); // make it return empty result
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

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
        assertThat(firstBindings.getNamedBindings()).isEmpty();

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
        assertThat(secondBindings.getNamedBindings()).isEmpty();

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
    void executeOperationWithBindByName() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery(query).build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        Statement statement = MockStatement.empty(); // make it return empty result
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        callback.invoke(statement, BIND_BY_NAME_METHOD, new Object[]{"$1", 100});
        callback.invoke(statement, BIND_NULL_BY_NAME_METHOD, new Object[]{"$2", String.class});
        callback.invoke(statement, ADD_METHOD, null);
        callback.invoke(statement, BIND_NULL_BY_NAME_METHOD, new Object[]{"$1", int.class});
        callback.invoke(statement, BIND_BY_NAME_METHOD, new Object[]{"$2", 200});
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
        assertThat(firstBindings.getNamedBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly("$1", "$2");

        List<BoundValue> boundValues = firstBindings.getNamedBindings().stream()
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
        assertThat(secondBindings.getNamedBindings())
            .hasSize(2)
            .extracting(Binding::getKey)
            .containsExactly("$1", "$2");

        boundValues = secondBindings.getNamedBindings().stream()
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

    @SuppressWarnings("unchecked")
    @Test
    void executeThenCancel() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("QUERY").build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();

        Statement statement = MockStatement.builder().result(MockResult.empty()).build();
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Flux<Result> result = Flux.from((Publisher<Result>) callback.invoke(statement, EXECUTE_METHOD, new Object[]{}))
            .flatMap(r -> Flux.from(r.map((row, metadata) -> row)).then(Mono.just(r)));

        StepVerifier.create(result)
            .expectSubscription()
            .expectNextCount(1)
            .thenCancel()// cancel after consuming one result
            .verify();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo).isNotNull();
        assertThat(afterQueryInfo.isSuccess())
            .as("Consuming at least one result is considered to query execution success")
            .isTrue();
    }

    @Test
    void executeThenImmediatelyCancel() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("QUERY").build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();

        Statement statement = MockStatement.builder().result(MockResult.empty()).build();
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, EXECUTE_METHOD, new Object[]{});

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .thenCancel()// immediately cancel
            .verify();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo).isNotNull();
        assertThat(afterQueryInfo.isSuccess())
            .as("Not consuming any result is considered to query execution failure")
            .isFalse();

    }

    @SuppressWarnings("unchecked")
    @Test
    void executeThenNext() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("QUERY").build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();

        Statement statement = MockStatement.builder().result(MockResult.empty()).build();
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, EXECUTE_METHOD, new Object[]{});

        // Flux.next() cancels upstream publisher
        Mono<?> mono = ((Flux<Result>) result).next();

        StepVerifier.create(mono)
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo)
            .as("Consuming one result without call .map or .getRowUpdated don't execute the query, so afterQuery is not called")
            .isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeThenNextWithMapCallOnResult() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("QUERY").build();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();

        Statement statement = MockStatement.builder().result(MockResult.empty()).build();
        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, EXECUTE_METHOD, new Object[]{});

        // Flux.next() cancels upstream publisher
        Mono<?> mono = ((Flux<Result>) result)
            .flatMap(r -> r.map((row, it) -> row))
            .next()
            .contextWrite(context -> context.put("foo.bar", "baz"));

        StepVerifier.create(mono)
            .expectSubscription()
            .expectNextCount(0).as("nothing on the flux since the result is empty. r.map return an empty publisher")
            .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(afterQueryInfo).isNotNull();
        assertThat(afterQueryInfo.isSuccess())
            .as("Consuming at least one result is considered to query execution success")
            .isTrue();
        assertThat(afterQueryInfo.getValueStore().get("foo.bar")).isEqualTo("baz");
    }

    @Test
    void unwrap() throws Throwable {
        Statement statement = MockStatement.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = new ProxyConfig();
        StatementInfo statementInfo = MockStatementInfo.empty();

        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(statement);
    }

    @Test
    void getProxyConfig() throws Throwable {
        Statement statement = MockStatement.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        ProxyConfig proxyConfig = new ProxyConfig();
        StatementInfo statementInfo = MockStatementInfo.empty();

        StatementCallbackHandler callback = new StatementCallbackHandler(statement, statementInfo, connectionInfo, proxyConfig);

        Object result = callback.invoke(statement, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

}
