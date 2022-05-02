/*
 * Copyright 2021 the original author or authors.
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

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.test.MockStatementInfo;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
class AfterQueryCallbackTest {

    private static final Logger logger = LoggerFactory.getLogger(AfterQueryCallbackTest.class);

    static class CountingListener implements ProxyExecutionListener {

        private final AtomicBoolean isBeforeQueryCalled = new AtomicBoolean();

        private final AtomicBoolean isAfterQueryCalled = new AtomicBoolean();

        private final AtomicInteger eachQueryResultCounter = new AtomicInteger();

        @Override
        public void eachQueryResult(QueryExecutionInfo execInfo) {
            this.eachQueryResultCounter.incrementAndGet();
            logger.info("eachQueryResult " + execInfo.getCurrentMappedResult());
        }

        @Override
        public void beforeQuery(QueryExecutionInfo execInfo) {
            this.isBeforeQueryCalled.set(true);
            logger.info("beforeQuery");
        }

        @Override
        public void afterQuery(QueryExecutionInfo execInfo) {
            this.isAfterQueryCalled.set(true);
            logger.info("afterQuery");
        }

        public boolean isBeforeQueryCalled() {
            return this.isBeforeQueryCalled.get();
        }

        public boolean isAfterQueryCalled() {
            return this.isAfterQueryCalled.get();
        }

        public int getEachQueryResultCount() {
            return this.eachQueryResultCounter.get();
        }
    }

    CountingListener listener;

    // publisher for the return of "Statement#execute"
    TestPublisher<Result> executePublisher;

    // publisher for the return of "Result#map"
    TestPublisher<String> resultPublisherForMap;

    // publisher for the return of "Result#getRowsUpdated"
    TestPublisher<Long> resultPublisherForGetRowsUpdated;

    // a mock that returns above test-publishers for #map and #getRowsUpdated methods.
    Result resultMock;


    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        this.listener = new CountingListener();
        this.executePublisher = TestPublisher.create();
        this.resultPublisherForMap = TestPublisher.create();
        this.resultPublisherForGetRowsUpdated = TestPublisher.create();

        this.resultMock = mock(Result.class);
        when(this.resultMock.map(any(BiFunction.class))).thenReturn(this.resultPublisherForMap);
        when(this.resultMock.getRowsUpdated()).thenReturn(this.resultPublisherForGetRowsUpdated);
    }

    // https://github.com/r2dbc/r2dbc-proxy/issues/94
    @Test
    void completeExecutePublisherWhileProcessingResultWithMap() {
        // This test performs the following scenario:
        // - "Statement#execute" publishes a "Result"
        // - "Result#map" publishes a String
        // - "Statement#execute" publisher completes
        // - "Result#map" publishes a String
        // - "Result#map" publisher completes

        Flux<String> flux = prepareFluxWithResultMap();
        flux.subscribe();

        // received a result
        this.executePublisher.next(this.resultMock);
        verifyListener(false, 0);

        // process the first result
        this.resultPublisherForMap.next("foo");
        verifyListener(false, 1);

        // now complete the publisher from Statement#execute
        this.executePublisher.complete();
        verifyListener(false, 1);

        // process the second result
        this.resultPublisherForMap.next("bar");
        verifyListener(false, 2);

        // complete the publisher for result
        this.resultPublisherForMap.complete();
        verifyListener(true, 2);
    }

    // https://github.com/r2dbc/r2dbc-proxy/issues/94
    @Test
    void completeExecutePublisherWhileProcessingResultWithGetRowUpdated() {
        // This test performs the following scenario:
        // - "Statement#execute" publishes a Result
        // - "Result#getRowsUpdated" publishes a number
        // - "Statement#execute" publisher completes
        // - "Result#getRowsUpdated" publisher completes

        Flux<Long> flux = prepareFluxWithResultGetRowsUpdated();
        flux.subscribe();

        // received a result
        this.executePublisher.next(this.resultMock);
        verifyListener(false, 0);

        // process the "getRowsUpdated" result
        this.resultPublisherForGetRowsUpdated.next(100L);
        verifyListener(false, 1);

        this.executePublisher.complete();
        verifyListener(false, 1);

        this.resultPublisherForGetRowsUpdated.complete();
        verifyListener(true, 1);
    }

    @SuppressWarnings("unchecked")
    private Flux<Long> prepareFluxWithResultGetRowsUpdated() {
        Statement mockStatement = mock(Statement.class);
        when((Publisher<Result>) mockStatement.execute()).thenReturn(this.executePublisher);

        ProxyFactory proxyFactory = createProxyFactory();
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("SELECT * FROM foo").build();
        Statement proxyStatement = proxyFactory.wrapStatement(mockStatement, statementInfo, new DefaultConnectionInfo());

        // perform "Statement#execute" and "Result#map"
        Flux<Long> flux = Flux.from(proxyStatement.execute()).flatMap(Result::getRowsUpdated);

        return flux;
    }

    @SuppressWarnings("unchecked")
    private Flux<String> prepareFluxWithResultMap() {
        Statement mockStatement = mock(Statement.class);
        when((Publisher<Result>) mockStatement.execute()).thenReturn(this.executePublisher);

        ProxyFactory proxyFactory = createProxyFactory();
        StatementInfo statementInfo = MockStatementInfo.builder().updatedQuery("SELECT * FROM foo").build();
        Statement proxyStatement = proxyFactory.wrapStatement(mockStatement, statementInfo, new DefaultConnectionInfo());

        // perform "Statement#execute" and "Result#map"
        Flux<String> flux = Flux.from(proxyStatement.execute()).flatMap(result ->
            result.map((row, metadata) ->
                // this lambda is skipped since the "resultMock" always return "resultPublisher"
                row.get("name", String.class)
            )
        );

        return flux;
    }

    private ProxyFactory createProxyFactory() {
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(this.listener).build();
        return new JdkProxyFactory(proxyConfig);
    }

    private void verifyListener(boolean isAfterQueryCalled, int eachQueryResultCount) {
        assertThat(this.listener.isAfterQueryCalled()).as("isAfterQueryCalled").isEqualTo(isAfterQueryCalled);
        assertThat(this.listener.getEachQueryResultCount()).as("eachQueryResultCount").isEqualTo(eachQueryResultCount);
    }

}
