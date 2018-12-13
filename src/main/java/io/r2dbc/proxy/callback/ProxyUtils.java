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

    @SuppressWarnings("unchecked")
    public static Optional<Connection> getOriginalConnection(Connection connection) {
        if (connection instanceof Wrapped) {
            return Optional.of(((Wrapped<Connection>) connection).unwrap());
        }
        return Optional.of(connection);
    }

    public static Optional<Connection> getOriginalConnection(Batch<?> batch) {
        if (batch instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) batch).getOriginalConnection());
        }
        return Optional.empty();
    }

    public static Optional<Connection> getOriginalConnection(Statement<?> statement) {
        if (statement instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) statement).getOriginalConnection());
        }
        return Optional.empty();
    }

    public static Optional<Connection> getOriginalConnection(Result result) {
        if (result instanceof ConnectionHolder) {
            return Optional.of(((ConnectionHolder) result).getOriginalConnection());
        }
        return Optional.empty();
    }


}
