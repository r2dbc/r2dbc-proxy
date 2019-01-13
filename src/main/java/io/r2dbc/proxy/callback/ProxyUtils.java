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

import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;

import java.util.Optional;

/**
 * Utility methods to obtain original {@link Connection} from proxy class.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyUtils {

    private ProxyUtils() {
    }

    /**
     * Get original {@link Connection} if given {@link Connection} has implemented {@link Wrapped}.
     *
     * @param connection a connection
     * @return optional of original connection or give connection
     * @throws IllegalArgumentException if {@code connection} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Optional<Connection> unwrapConnection(Connection connection) {
        Assert.requireNonNull(connection, "connection must not be null");

        if (connection instanceof Wrapped) {
            return Optional.of(((Wrapped<Connection>) connection).unwrap());
        }
        return Optional.of(connection);
    }

    /**
     * Get original {@link Connection} from proxy {@link Batch}.
     *
     * When provided {@link Batch} is a proxy that implements {@link ConnectionHolder}, retrieves original
     * {@link Connection}; otherwise, returns empty {@code Optional}.
     *
     * @param batch a batch
     * @return optional of original connection or empty
     * @throws IllegalArgumentException if {@code batch} is {@code null}
     */
    public static Optional<Connection> unwrapConnection(Batch<?> batch) {
        Assert.requireNonNull(batch, "batch must not be null");

        if (batch instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) batch).unwrapConnection());
        }
        return Optional.empty();
    }

    /**
     * Get original {@link Connection} from proxy {@link Statement}.
     *
     * When provided {@link Statement} is a proxy that implements {@link ConnectionHolder}, retrieves original
     * {@link Connection}; otherwise, returns empty {@code Optional}.
     *
     * @param statement a statement
     * @return optional of original connection or empty
     * @throws IllegalArgumentException if {@code statement} is {@code null}
     */
    public static Optional<Connection> unwrapConnection(Statement<?> statement) {
        Assert.requireNonNull(statement, "statement must not be null");

        if (statement instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) statement).unwrapConnection());
        }
        return Optional.empty();
    }

    /**
     * Get original {@link Connection} from proxy {@link Result}.
     *
     * When provided {@link Result} is a proxy that implements {@link ConnectionHolder}, retrieves original
     * {@link Connection}; otherwise, returns empty {@code Optional}.
     *
     * @param result a statement
     * @return optional of original connection or empty
     * @throws IllegalArgumentException if {@code result} is {@code null}
     */
    public static Optional<Connection> unwrapConnection(Result result) {
        Assert.requireNonNull(result, "result must not be null");

        if (result instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) result).unwrapConnection());
        }
        return Optional.empty();
    }

}
