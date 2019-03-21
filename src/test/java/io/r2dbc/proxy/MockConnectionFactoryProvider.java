/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy;

import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Mock implementation of {@link ConnectionFactoryProvider} for testing.
 *
 * <p>This class provides static access to the callbacks for implemented {@link ConnectionFactory} methods.
 * When this class is registered to {@link ServiceLoader} discovery mechanism, those static methods can set
 * behavior for the callbacks and have access to the invoked parameters.
 *
 * @author Tadaya Tsuyukubo
 * @see ServiceLoader
 */
public class MockConnectionFactoryProvider implements ConnectionFactoryProvider {

    private static Function<ConnectionFactoryOptions, ConnectionFactory> DEFAULT_CREATE_CALLBACK = connectionFactoryOptions -> null;

    private static Function<ConnectionFactoryOptions, Boolean> DEFAULT_SUPPORTS_CALLBACK = connectionFactoryOptions -> false;

    private static Function<ConnectionFactoryOptions, ConnectionFactory> createCallback = DEFAULT_CREATE_CALLBACK;

    private static Function<ConnectionFactoryOptions, Boolean> supportsCallback = DEFAULT_SUPPORTS_CALLBACK;

    /**
     * Reset the registered callbacks to the default.
     */
    public static void reset() {
        createCallback = DEFAULT_CREATE_CALLBACK;
        supportsCallback = DEFAULT_SUPPORTS_CALLBACK;
    }

    /**
     * Make {@link #supports(ConnectionFactoryOptions)} always return {@code true}.
     */
    public static void setSupportsAlways() {
        supportsCallback = connectionFactoryOptions -> true;
    }

    /**
     * Make {@link #create(ConnectionFactoryOptions)} to return the given {@link ConnectionFactory} instance.
     *
     * @param connectionFactory connectionFactory to return
     * @throws IllegalArgumentException if {@code connectionFactory} is {@code null}
     */
    public static void setCreateCallbackReturn(ConnectionFactory connectionFactory) {
        Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");
        createCallback = connectionFactoryOptions -> connectionFactory;
    }

    /**
     * Set the callback which is invoked when {@link #supports(ConnectionFactoryOptions)} is called.
     *
     * @param callback a callback for {@link #supports(ConnectionFactoryOptions)}
     * @throws IllegalArgumentException if {@code callback} is {@code null}
     */
    public static void setSupportsCallback(Function<ConnectionFactoryOptions, Boolean> callback) {
        supportsCallback = Assert.requireNonNull(callback, "callback must not be null");
    }

    /**
     * Set the callback which is invoked when {@link #create(ConnectionFactoryOptions)} is called.
     *
     * @param callback a callback for {@link #create(ConnectionFactoryOptions)}
     * @throws IllegalArgumentException if {@code callback} is {@code null}
     */
    public static void setCreateCallback(Function<ConnectionFactoryOptions, ConnectionFactory> callback) {
        createCallback = Assert.requireNonNull(callback, "callback must not be null");
    }

    @Override
    public ConnectionFactory create(ConnectionFactoryOptions connectionFactoryOptions) {
        return createCallback.apply(connectionFactoryOptions);
    }

    @Override
    public boolean supports(ConnectionFactoryOptions connectionFactoryOptions) {
        return supportsCallback.apply(connectionFactoryOptions);
    }

    @Override
    public String getDriver() {
        return ProxyConnectionFactoryProvider.PROXY_DRIVER;
    }
}
