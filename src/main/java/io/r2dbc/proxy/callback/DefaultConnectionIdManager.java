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
import io.r2dbc.spi.Connection;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link ConnectionIdManager}.
 *
 * This implementation uses increasing long as connection id.
 *
 * @author Tadaya Tsuyukubo
 */
final class DefaultConnectionIdManager implements ConnectionIdManager {

    private AtomicLong idCounter = new AtomicLong(0);

    @Override
    public String getId(Connection connection) {
        Assert.requireNonNull(connection, "connection must not be null");

        return String.valueOf(this.idCounter.incrementAndGet());
    }

}
