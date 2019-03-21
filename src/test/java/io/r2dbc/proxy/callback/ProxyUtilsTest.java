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

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.test.MockBatch;
import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyUtilsTest {

    @Test
    void unwrapConnection() {
        Connection originalConnection = MockConnection.empty();
        Batch originalBatch = MockBatch.empty();
        Statement originalStatement = MockStatement.empty();
        Result originalResult = MockResult.empty();

        String query = "QUERY";

        ProxyConfig proxyConfig = new ProxyConfig();

        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        connectionInfo.setOriginalConnection(originalConnection);

        Connection proxyConnection = proxyConfig.getProxyFactory().wrapConnection(originalConnection, connectionInfo);

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(connectionInfo);

        Batch proxyBatch = proxyConfig.getProxyFactory().wrapBatch(originalBatch, connectionInfo);
        Statement proxyStatement = proxyConfig.getProxyFactory().wrapStatement(originalStatement, query, connectionInfo);
        Result proxyResult = proxyConfig.getProxyFactory().wrapResult(originalResult, queryExecutionInfo);

        Optional<Connection> result;

        result = ProxyUtils.unwrapConnection(proxyConnection);
        assertThat(result).hasValue(originalConnection);

        result = ProxyUtils.unwrapConnection(proxyBatch);
        assertThat(result).hasValue(originalConnection);

        result = ProxyUtils.unwrapConnection(proxyStatement);
        assertThat(result).hasValue(originalConnection);

        result = ProxyUtils.unwrapConnection(proxyResult);
        assertThat(result).hasValue(originalConnection);
    }

}
