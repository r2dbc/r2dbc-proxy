/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy;

import io.r2dbc.proxy.listener.LifeCycleListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;
import io.r2dbc.spi.Option;

import java.util.Set;

import static java.lang.String.format;

/**
 * An implementation of {@link ConnectionFactoryProvider} for creating proxy {@link ConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryProvider implements ConnectionFactoryProvider {

    /**
     * Driver option value.
     */
    public static final String PROXY_DRIVER = "proxy";

    /**
     * Fully qualified proxy listener class names.
     *
     * Specified class names need to be implementations of ({@link ProxyExecutionListener} or {@link LifeCycleListener}) interfaces.
     */
    public static final Option<Set<String>> PROXY_LISTENERS = Option.valueOf("proxyListener");

    private static final String COLON = ":";

    /**
     * Create a new proxy {@link ConnectionFactory} from given {@link ConnectionFactoryOptions}.
     *
     * @param connectionFactoryOptions a collection of {@link ConnectionFactoryOptions}
     * @return the proxy {@link ConnectionFactory}
     * @throws IllegalArgumentException if {@code connectionFactoryOptions} is {@code null}
     * @throws IllegalStateException    if there is no value for {@link ConnectionFactoryOptions#PROTOCOL}
     * @throws IllegalArgumentException if {@link ConnectionFactoryOptions#PROTOCOL} has invalid format
     * @throws IllegalArgumentException if specified proxyListener parameter class cannot be found or instantiated
     * @throws IllegalArgumentException if specified proxyListener parameter value is not a proxy listener class
     */
    @Override
    public ConnectionFactory create(ConnectionFactoryOptions connectionFactoryOptions) {
        Assert.requireNonNull(connectionFactoryOptions, "connectionFactoryOptions must not be null");

        // To delegate to the next factory provider, inspect the PROTOCOL and convert it to the next DRIVER and PROTOCOL values.
        //
        // example:
        //   | Property | Input        | Output     |
        //   |----------|--------------|------------|
        //   | DRIVER   | proxy        | tc         |
        //   | PROTOCOL | tc:mysql:udp | mysql:udp  |
        //   |----------|--------------|------------|
        //   | DRIVER   | proxy        | postgres   |
        //   | PROTOCOL | postgres     | <empty>    |

        String protocol = connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PROTOCOL);
        if (protocol.trim().length() == 0) {
            throw new IllegalArgumentException(format("Protocol %s is not valid.", protocol));
        }
        String[] protocols = protocol.split(COLON, 2);
        String driverDelegate = protocols[0];

        // when protocol does NOT contain COLON, the length becomes 1
        String protocolDelegate = protocols.length == 2 ? protocols[1] : "";

        ConnectionFactoryOptions newOptions = ConnectionFactoryOptions.builder()
            .from(connectionFactoryOptions)
            .option(ConnectionFactoryOptions.DRIVER, driverDelegate)
            .option(ConnectionFactoryOptions.PROTOCOL, protocolDelegate)
            .build();


        // Run discovery again to find the actual connection factory
        ConnectionFactory connectionFactory = ConnectionFactories.find(newOptions);
        if (connectionFactory == null) {
            return null;
        }

        ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);

        // add proxy listeners if specified
        if (connectionFactoryOptions.hasOption(PROXY_LISTENERS)) {
            connectionFactoryOptions.getValue(PROXY_LISTENERS).forEach(proxyListener -> {

                Object listener;
                try {
                    Class<?> proxyListenerClass = Class.forName(proxyListener);
                    listener = proxyListenerClass.newInstance();
                } catch (Exception e) {
                    String message = proxyListener + " is not a valid proxy listener class";
                    throw new IllegalArgumentException(message, e);
                }

                // currently two types of listeners exist
                if (listener instanceof ProxyExecutionListener) {
                    builder.listener((ProxyExecutionListener) listener);
                } else if (listener instanceof LifeCycleListener) {
                    builder.listener((LifeCycleListener) listener);
                } else {
                    throw new IllegalArgumentException(proxyListener + " is not a proxy listener class");
                }

            });
        }

        return builder.create();
    }

    @Override
    public boolean supports(ConnectionFactoryOptions connectionFactoryOptions) {
        Assert.requireNonNull(connectionFactoryOptions, "connectionFactoryOptions must not be null");

        String driver = connectionFactoryOptions.getValue(ConnectionFactoryOptions.DRIVER);
        return PROXY_DRIVER.equals(driver);
    }

}
