/*
 * Copyright 2026 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Query lifecycle tests for filtered results.
 */
class ResultFilterIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final ProxyExecutionListener listener = mock(ProxyExecutionListener.class);

    @ParameterizedTest(name = "{0}, filters={1}")
    @MethodSource("rowMappings")
    void filteredRowsCompleteOriginalQuery(String method, int filters) {
        StepVerifier.create(query("SELECT 42", result -> {
            Result filtered = result.filter(segment -> segment instanceof Result.RowSegment);
            if (filters == 2) {
                filtered = filtered.filter(segment -> true);
            }
            verify(this.listener, never()).afterQuery(any());
            switch (method) {
                case "mapFunction":
                    return filtered.map(readable -> readable.get(0, Integer.class));
                case "mapBiFunction":
                    return filtered.map((row, metadata) -> row.get(0, Integer.class));
                case "flatMap":
                    return filtered.flatMap(segment -> Mono.just(((Result.RowSegment) segment).row().get(0, Integer.class)));
                default:
                    throw new IllegalArgumentException(method);
            }
        }))
            .expectNext(42)
            .expectComplete()
            .verify(TIMEOUT);

        assertQueryCompleted(true);
        verify(this.listener).eachQueryResult(any());
    }

    static Stream<Arguments> rowMappings() {
        return Stream.of("mapFunction", "mapBiFunction", "flatMap")
            .flatMap(method -> Stream.of(1, 2).map(filters -> arguments(method, filters)));
    }

    @Test
    void filteredUpdateCountCompletesOriginalQuery() {
        StepVerifier.create(query("CREATE TABLE filtered_result (id INT)", result -> result
            .filter(segment -> segment instanceof Result.UpdateCount)
            .filter(segment -> true)
            .getRowsUpdated()))
            .expectNextCount(1)
            .expectComplete()
            .verify(TIMEOUT);

        assertQueryCompleted(true);
    }

    @Test
    void emptyFilteredResultCompletesOriginalQuery() {
        StepVerifier.create(query("SELECT 42", result -> result
            .filter(segment -> false)
            .flatMap(segment -> Mono.just(42))))
            .expectComplete()
            .verify(TIMEOUT);

        assertQueryCompleted(true);
        verify(this.listener, never()).eachQueryResult(any());
    }

    @Test
    void recoveredFilteredResultMappingErrorCompletesOriginalQuery() {
        IllegalStateException failure = new IllegalStateException("mapping failed");
        StepVerifier.create(query("SELECT 42", result -> Flux.from(result
            .filter(segment -> segment instanceof Result.RowSegment)
            .flatMap(segment -> Mono.<Integer>error(failure)))
            .onErrorResume(error -> {
                assertThat(error).isSameAs(failure);
                return Mono.empty();
            })))
            .expectComplete()
            .verify(TIMEOUT);

        QueryExecutionInfo executionInfo = assertQueryCompleted(false);
        assertThat(executionInfo.getThrowable()).isSameAs(failure);
    }

    @Test
    void cancelledFilteredResultCompletesOriginalQuery() {
        StepVerifier.create(query("SELECT 42 UNION ALL SELECT 43", result -> Flux.from(result
            .filter(segment -> segment instanceof Result.RowSegment)
            .map(readable -> readable.get(0, Integer.class)))
            .take(1)))
            .expectNext(42)
            .expectComplete()
            .verify(TIMEOUT);

        assertQueryCompleted(true);
        verify(this.listener).eachQueryResult(any());
    }

    private <T> Flux<T> query(String sql, Function<Result, Publisher<T>> mapping) {
        ConnectionFactory original = ConnectionFactories.get("r2dbc:h2:mem:///filter_" + UUID.randomUUID());
        ConnectionFactory factory = ProxyConnectionFactory.builder(original).listener(this.listener).build();
        return Flux.usingWhen(Mono.from(factory.create()),
            connection -> Flux.from(connection.createStatement(sql).execute())
                .collectList()
                .flatMapMany(results -> Flux.fromIterable(results).flatMap(mapping)),
            connection -> Mono.from(connection.close()));
    }

    private QueryExecutionInfo assertQueryCompleted(boolean success) {
        ArgumentCaptor<QueryExecutionInfo> beforeQuery = ArgumentCaptor.forClass(QueryExecutionInfo.class);
        verify(this.listener).beforeQuery(beforeQuery.capture());
        QueryExecutionInfo executionInfo = beforeQuery.getValue();
        verify(this.listener).afterQuery(executionInfo);
        assertThat(executionInfo.isSuccess()).isEqualTo(success);
        return executionInfo;
    }
}
