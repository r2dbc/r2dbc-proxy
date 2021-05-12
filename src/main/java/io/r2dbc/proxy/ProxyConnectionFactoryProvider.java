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

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;
import io.r2dbc.spi.Option;

import java.util.Arrays;
import java.util.Collection;

import static java.lang.String.format;

/**
 * An implementation of {@link ConnectionFactoryProvider} for creating proxy {@link ConnectionFactory}.
 *
 * <p>This provider takes {@link #PROXY_LISTENERS proxyListener} parameter and its value can be:
 * <ul>
 * <li> Fully qualified proxy listener class name
 * <li> Proxy listener {@link Class}
 * <li> Proxy listener instance
 * <li> {@link Collection} of above
 * </ul>
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryProvider implements ConnectionFactoryProvider {

    /**
     * Driver option value.
     */
    public static final String PROXY_DRIVER = "proxy";

    /**
     * Proxy listener {@link Option}.
     */
    public static final Option<Object> PROXY_LISTENERS = Option.valueOf("proxyListener");

    private static final String COLON = ":";

    /**
     * Create a new proxy {@link ConnectionFactory} from given {@link ConnectionFactoryOptions}.
     *
     * @param connectionFactoryOptions a collection of {@link ConnectionFactoryOptions}
     * @return the proxy {@link ConnectionFactory}
     * @throws IllegalArgumentException if {@code connectionFactoryOptions} is {@code null}
     * @throws IllegalStateException    if there is no value for {@link ConnectionFactoryOptions#PROTOCOL}
     * @throws IllegalArgumentException if {@link ConnectionFactoryOptions#PROTOCOL} has invalid format
     * @throws IllegalArgumentException if delegating {@link ConnectionFactory} cannot be found
     * @throws IllegalArgumentException if specified proxyListener parameter class cannot be found or instantiated
     * @throws IllegalArgumentException if specified proxyListener parameter value is not a proxy listener class or instance
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

        Object protocolValue = connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PROTOCOL);
        if (!(protocolValue instanceof String)) {
            throw new IllegalArgumentException(format("Protocol %s is not String.", protocolValue));
        }
        String protocol = (String) protocolValue;
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
            throw new IllegalArgumentException(format("Could not find delegating driver %s", driverDelegate));
        }

        ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);

        // add proxy listeners if specified
        if (connectionFactoryOptions.hasOption(PROXY_LISTENERS)) {
            Object proxyListenerOption = connectionFactoryOptions.getValue(PROXY_LISTENERS);
            registerProxyListeners(proxyListenerOption, builder);
        }

        return builder.build();
    }

    @SuppressWarnings("deprecation")
    private void registerProxyListeners(Object optionValue, ProxyConnectionFactory.Builder builder) {
        if (optionValue instanceof Collection) {
            ((Collection<?>) optionValue).forEach(element -> registerProxyListeners(element, builder));
        } else if (optionValue instanceof String) {
            Arrays.asList(((String) optionValue).split(",", -1))
                    .forEach(className -> registerProxyListenerClassName(className, builder));
        } else if (optionValue instanceof Class<?>) {
            registerProxyListenerClass((Class<?>) optionValue, builder);
        } else if (optionValue instanceof ProxyExecutionListener) {
            builder.listener((ProxyExecutionListener) optionValue);
        } else {
            throw new IllegalArgumentException(optionValue + " is not a proxy listener instance");
        }
    }

    private void registerProxyListenerClassName(String proxyListenerClassName, ProxyConnectionFactory.Builder builder) {
        Class<?> proxyListenerClass;
        try {
            proxyListenerClass = Class.forName(proxyListenerClassName);
        } catch (Exception e) {
            String message = proxyListenerClassName + " is not a valid proxy listener class";
            throw new IllegalArgumentException(message, e);
        }
        registerProxyListeners(proxyListenerClass, builder);
    }

    private void registerProxyListenerClass(Class<?> proxyListenerClass, ProxyConnectionFactory.Builder builder) {
        Object proxyListenerInstance;
        try {
            proxyListenerInstance = proxyListenerClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(format("Could not instantiate %s", proxyListenerClass), e);
        }
        registerProxyListeners(proxyListenerInstance, builder);
    }

    @Override
    public boolean supports(ConnectionFactoryOptions connectionFactoryOptions) {
        Assert.requireNonNull(connectionFactoryOptions, "connectionFactoryOptions must not be null");

        Object driver = connectionFactoryOptions.getValue(ConnectionFactoryOptions.DRIVER);
        return PROXY_DRIVER.equals(driver);
    }

    @Override
    public String getDriver() {
        return PROXY_DRIVER;
    }

}
