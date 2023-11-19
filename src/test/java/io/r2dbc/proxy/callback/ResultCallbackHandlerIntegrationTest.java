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
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Integration test for {@link ResultCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
class ResultCallbackHandlerIntegrationTest {

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
    void resultMapWithFunction() {
        AfterQueryListener listener = new AfterQueryListener();
        ConnectionFactory proxyConnectionFactory = createProxyConnectionFactory(listener);

        // with "Result#map(Function)"
        Function<Result, Publisher<String>> mapping = (result) ->
            result.map((readable) -> readable.get("name", String.class));
        String output = queryWithResultMapping(proxyConnectionFactory, mapping);

        assertThat(output).isEqualTo("Bar");
        assertThat(listener.isCalled()).isTrue();
    }

    @Test
    void resultMapWithBiFunction() {
        AfterQueryListener listener = new AfterQueryListener();
        ConnectionFactory proxyConnectionFactory = createProxyConnectionFactory(listener);

        // with "Result#map(BiFunction)"
        Function<Result, Publisher<String>> mapping = (result) ->
            result.map((row, rowMetadata) -> row.get("name", String.class));
        String output = queryWithResultMapping(proxyConnectionFactory, mapping);

        assertThat(output).isEqualTo("Bar");
        assertThat(listener.isCalled()).isTrue();
    }

    @Test
    void resultFlatMap() {
        AfterQueryListener listener = new AfterQueryListener();
        ConnectionFactory proxyConnectionFactory = createProxyConnectionFactory(listener);

        // with "Result#flatMap(Function)"
        Function<Result, Publisher<String>> mapping = (result) ->
            result.flatMap((segment) -> Flux.just(((Result.RowSegment) segment).row().get("name", String.class)));
        String output = queryWithResultMapping(proxyConnectionFactory, mapping);

        assertThat(output).isEqualTo("Bar");
        assertThat(listener.isCalled()).isTrue();
    }

    private ConnectionFactory createProxyConnectionFactory(ProxyExecutionListener listener) {
        String r2dbcUrl = "r2dbc:h2:mem://sa@/testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        ConnectionFactory connectionFactory = ConnectionFactories.get(r2dbcUrl);
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        return ProxyConnectionFactory.builder(connectionFactory, proxyConfig).build();
    }

    private String queryWithResultMapping(ConnectionFactory proxyConnectionFactory, Function<Result, Publisher<String>> mapping) {
        // @formatter:off
        String output = Mono.from(proxyConnectionFactory.create())
            .flatMapMany(connection -> Mono.from(connection
                .createStatement("SELECT id, name FROM emp WHERE id = ?")
                .bind(0, 20)
                .execute()))
            .flatMap(mapping)
            .single()
            .subscribeOn(Schedulers.boundedElastic())
            .log()
            .block();
        // @formatter:on
        return output;
    }

    static class AfterQueryListener implements ProxyExecutionListener {

        private boolean called;

        @Override
        public void afterQuery(QueryExecutionInfo execInfo) {
            this.called = true;
        }

        public boolean isCalled() {
            return this.called;
        }
    }
}
