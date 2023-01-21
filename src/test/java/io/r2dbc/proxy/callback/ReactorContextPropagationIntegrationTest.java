/*
 * Copyright 2023 the original author or authors.
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

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.core.BindInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.core.ValueStore;
import io.r2dbc.proxy.listener.BindParameterConverter;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * @author Tadaya Tsuyukubo
 */
class ReactorContextPropagationIntegrationTest {

    private static EmbeddedDatabase db;

    @BeforeAll
    static void setUpData() {
        db = new EmbeddedDatabaseBuilder()
            .setType(H2)
            .ignoreFailedDrops(true)
            .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        jdbcTemplate.execute("CREATE TABLE emp(id INT, name VARCHAR(20))");
        jdbcTemplate.execute("INSERT INTO emp VALUES (10, 'Foo')");
        jdbcTemplate.execute("INSERT INTO emp VALUES (20, 'Bar')");
        jdbcTemplate.execute("INSERT INTO emp VALUES (30, 'Baz')");
    }

    @AfterAll
    static void tearDown() {
        db.shutdown();
    }


    @Test
    void contextViewPropagation() {
        String r2dbcUrl = "r2dbc:h2:mem://sa@/testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        ConnectionFactory connectionFactory = ConnectionFactories.get(r2dbcUrl);

        AtomicReference<ValueStore> onCreateStatementStatementInfo = new AtomicReference<>();
        AtomicReference<ValueStore> onCreateStatementConnectionInfo = new AtomicReference<>();
        AtomicReference<ValueStore> onBindStatementInfo = new AtomicReference<>();
        AtomicReference<ValueStore> onBindConnectionInfo = new AtomicReference<>();

        Map<String, ValueStore> methodExecutionInfo = new HashMap<>();
        Map<String, ValueStore> methodConnectionInfo = new HashMap<>();

        AtomicReference<ValueStore> queryExecutionInfo = new AtomicReference<>();
        AtomicReference<ValueStore> queryConnectionInfo = new AtomicReference<>();

        BindParameterConverter bindParameterConverter = new BindParameterConverter() {

            @Override
            public String onCreateStatement(String query, StatementInfo info) {
                onCreateStatementStatementInfo.set(info.getValueStore());
                onCreateStatementConnectionInfo.set(info.getConnectionInfo().getValueStore());
                return query;
            }

            @Override
            public void onBind(BindInfo info, Statement proxyStatement, BindOperation defaultBinding) {
                onBindStatementInfo.set(info.getStatementInfo().getValueStore());
                onBindConnectionInfo.set(info.getStatementInfo().getConnectionInfo().getValueStore());
                defaultBinding.proceed();  // just perform default behavior
            }
        };

        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                String methodName = executionInfo.getMethod().getName();
                methodExecutionInfo.put(methodName, executionInfo.getValueStore());
                methodConnectionInfo.put(methodName, executionInfo.getConnectionInfo().getValueStore());
            }

            @Override
            public void beforeQuery(QueryExecutionInfo execInfo) {
                queryExecutionInfo.set(execInfo.getValueStore());
                queryConnectionInfo.set(execInfo.getConnectionInfo().getValueStore());
            }
        };

        ProxyConfig proxyConfig = ProxyConfig.builder().bindParameterConverter(bindParameterConverter).listener(listener).build();
        ConnectionFactory proxyConnectionFactory = ProxyConnectionFactory.builder(connectionFactory, proxyConfig).build();

        // @formatter:off
        String output = Mono.from(proxyConnectionFactory.create()).contextWrite(context -> context.put("foo", "FOO")) // connection level context
            .flatMapMany(connection -> Mono.from(connection
                .createStatement("SELECT id, name FROM emp WHERE id = ?")
                .bind(0, 20)
                .execute()).contextWrite(context -> context.put("bar", "BAR")))  // query exec level context
            .flatMap(result -> Mono.from(result
                .map((row, rowMetadata) -> row.get("name", String.class)))
                .contextWrite(context -> context.put("qux", "QUX"))  // Result#map level context
            )
            .single()
            .subscribeOn(Schedulers.boundedElastic())
            .contextWrite(context -> context.put("baz", "BAZ"))  // whole flow context
            .log()
            .block();
        // @formatter:on

        assertThat(output).isEqualTo("Bar");

        // For BindParameterConverter#onCreateStatement
        validateValueSource(onCreateStatementStatementInfo);  // empty
        validateValueSource(onCreateStatementConnectionInfo, "baz", "foo");

        // For BindParameterConverter#onBind
        validateValueSource(onBindStatementInfo);  // empty
        validateValueSource(onBindConnectionInfo, "baz", "foo");

        // For ProxyExecutionListener query execution
        validateValueSource(queryExecutionInfo, "baz", "bar");
        validateValueSource(queryConnectionInfo, "baz", "foo");

        // For ProxyExecutionListener method execution (method level ValueStore)
        assertThat(methodExecutionInfo).containsOnlyKeys("create", "createStatement", "bind", "execute", "map", "get");
        validateValueSource(methodExecutionInfo.get("create"), "baz", "foo");
        validateValueSource(methodExecutionInfo.get("createStatement"));
        validateValueSource(methodExecutionInfo.get("bind"));
        validateValueSource(methodExecutionInfo.get("execute"), "baz", "bar");
        validateValueSource(methodExecutionInfo.get("map"), "baz", "qux");
        validateValueSource(methodExecutionInfo.get("get"));

        // For ProxyExecutionListener method execution (connection level ValueStore - should be always same key/value)
        assertThat(methodConnectionInfo).containsOnlyKeys("create", "createStatement", "bind", "execute", "map", "get");
        validateValueSource(methodConnectionInfo.get("create"), "baz", "foo");
        validateValueSource(methodConnectionInfo.get("createStatement"), "baz", "foo");
        validateValueSource(methodConnectionInfo.get("bind"), "baz", "foo");
        validateValueSource(methodConnectionInfo.get("execute"), "baz", "foo");
        validateValueSource(methodConnectionInfo.get("map"), "baz", "foo");
        validateValueSource(methodConnectionInfo.get("get"), "baz", "foo");
    }

    private void validateValueSource(AtomicReference<ValueStore> holder, String... expectedContextKeys) {
        assertThat(holder).hasValueSatisfying(store -> validateValueSource(store, expectedContextKeys));
    }

    private void validateValueSource(ValueStore store, String... expectedContextKeys) {
        ContextView view = store.get(ContextView.class, ContextView.class);
        if (expectedContextKeys.length == 0) {
            assertThat(view).isNull();
        } else {
            assertThat(view).isNotNull();
            for (String key : expectedContextKeys) {
                assertThat(view.hasKey(key)).isTrue();
            }
        }
    }

}
