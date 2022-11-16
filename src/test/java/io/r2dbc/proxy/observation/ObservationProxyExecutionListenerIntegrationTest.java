/*
 * Copyright 2022 the original author or authors.
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

package io.r2dbc.proxy.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpanAssert;
import io.micrometer.tracing.test.simple.SpansAssert;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Integration test for {@link ObservationProxyExecutionListener}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ObservationProxyExecutionListenerIntegrationTest extends SampleTestRunner {

    public ObservationProxyExecutionListenerIntegrationTest() {
        super(SampleRunnerConfig.builder().build());
    }

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

    @Override
    public SampleTestRunnerConsumer yourCode() throws Exception {
        return (bb, meterRegistry) -> {
            Span parentSpan = bb.getTracer().currentSpan();
            Assert.requireNonNull(parentSpan, "parentSpan must not be null");
            String parentTraceId = parentSpan.context().traceId();
            String parentSpanId = parentSpan.context().spanId();

            String result = doLogic();
            assertThat(result).isEqualTo("Bar");

            // @formatter:off
            SpansAssert.assertThat(bb.getFinishedSpans())
                .hasASpanWithRemoteServiceName("H2")
                .hasNumberOfSpansEqualTo(1)
                .singleElement()
                .satisfies(span -> {
                    SpanAssert.assertThat(span)
                        .hasKindEqualTo(Span.Kind.CLIENT)
                        .hasTag("r2dbc.connection", "H2")
                        .hasTag("r2dbc.query[0]", "SELECT id, name FROM emp WHERE id = ?")
                        .hasTag("r2dbc.params[0]", "(20)")
                        .hasTagWithKey("r2dbc.thread");
                    assertThat(span.getTags().get("r2dbc.thread")).containsIgnoringCase("boundedElastic");
                    assertThat(span.getTraceId()).isEqualTo(parentTraceId);
                    assertThat(span.getParentId()).isEqualTo(parentSpanId);
                });
            // @formatter:on
        };
    }

    @Nullable
    private String doLogic() throws Exception {
        String r2dbcUrl = "r2dbc:h2:mem://sa@/testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        ConnectionFactory connectionFactory = ConnectionFactories.get(r2dbcUrl);
        ObservationProxyExecutionListener listener = new ObservationProxyExecutionListener(getObservationRegistry(), connectionFactory, r2dbcUrl);
        listener.setIncludeParameterValues(true);
        ConnectionFactory proxyConnectionFactory = ProxyConnectionFactory.builder(connectionFactory).listener(listener).build();

        Observation parentObservation = getObservationRegistry().getCurrentObservation();
        Assert.requireNonNull(parentObservation, "parentObservation must not be null");

        // @formatter:off
        return Mono.from(proxyConnectionFactory.create())
            .flatMapMany(connection -> connection
                .createStatement("SELECT id, name FROM emp WHERE id = ?")
                .bind(0, 20)
                .execute())
            .flatMap(result -> result
                .map((row, rowMetadata) -> row.get("name", String.class)))
            .single()
            .subscribeOn(Schedulers.boundedElastic())
            .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parentObservation))
            .log()
            .block();
        // @formatter:on
    }
}
