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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;

/**
 * Defines factory methods to create proxy of SPI classes.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyFactory {

    void setProxyConfig(ProxyConfig proxyConfig);

    ConnectionFactory createProxyConnectionFactory(ConnectionFactory connectionFactory);

    Connection createProxyConnection(Connection connection, ConnectionInfo connectionInfo);

    Batch<?> createProxyBatch(Batch<?> batch, ConnectionInfo connectionInfo);

    Statement<?> createProxyStatement(Statement<?> statement, String query, ConnectionInfo connectionInfo);

    Result createProxyResult(Result result, QueryExecutionInfo executionInfo);

}
