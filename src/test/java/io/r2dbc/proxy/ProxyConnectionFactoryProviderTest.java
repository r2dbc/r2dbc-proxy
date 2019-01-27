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

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.r2dbc.proxy.ProxyConnectionFactoryProvider.PROXY_LISTENERS;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ProxyConnectionFactoryProvider}
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryProviderTest {

    private ProxyConnectionFactoryProvider provider = new ProxyConnectionFactoryProvider();

    @BeforeEach
    void setUp() {
        MockConnectionFactoryProvider.reset();
        TestProxyExecutionListener.reset();
    }

    @Test
    void doesNotSupportWithWrongDriver() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, "non-proxy")
            .build())).isFalse();
    }

    @Test
    void doesNotSupportWithoutDriver() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .build())).isFalse();
    }

    @Test
    void supports() {
        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .build())).isTrue();

        assertThat(this.provider.supports(ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(HOST, "my-host")
            .build())).isTrue();
    }


    @Test
    @SuppressWarnings("unchecked")
    void create() {
        AtomicReference<ConnectionFactoryOptions> receivedOptionsHolder = new AtomicReference<>();

        MockConnectionFactory connectionFactory = MockConnectionFactory.empty();
        MockConnectionFactoryProvider.setSupportsAlways();
        MockConnectionFactoryProvider.setCreateCallback(connectionFactoryOptions -> {
            receivedOptionsHolder.set(connectionFactoryOptions);
            return connectionFactory;
        });

        ConnectionFactory factory = this.provider.create(ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "foo")
            .build());

        assertThat(factory).isInstanceOf(Wrapped.class);
        ConnectionFactory unwrapped = ((Wrapped<ConnectionFactory>) factory).unwrap();
        assertThat(unwrapped).isSameAs(connectionFactory);

        ConnectionFactoryOptions receivedOptions = receivedOptionsHolder.get();
        String receivedDriver = receivedOptions.getValue(DRIVER);
        String receivedProtocol = receivedOptions.getValue(PROTOCOL);

        assertThat(receivedDriver).isEqualTo("foo");
        assertThat(receivedProtocol).isEmpty();
    }

    @Test
    void createWithNestedProtocol() {
        AtomicReference<ConnectionFactoryOptions> receivedOptionsHolder = new AtomicReference<>();

        MockConnectionFactoryProvider.setSupportsAlways();
        MockConnectionFactoryProvider.setCreateCallback(connectionFactoryOptions -> {
            receivedOptionsHolder.set(connectionFactoryOptions);
            return MockConnectionFactory.empty();
        });

        this.provider.create(ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "foo:bar:baz")
            .build());

        ConnectionFactoryOptions receivedOptions = receivedOptionsHolder.get();
        String receivedDriver = receivedOptions.getValue(DRIVER);
        String receivedProtocol = receivedOptions.getValue(PROTOCOL);

        assertThat(receivedDriver).isEqualTo("foo");
        assertThat(receivedProtocol).isEqualTo("bar:baz");
    }

    @Test
    void createWithValidListener() {
        MockConnectionFactory connectionFactory = MockConnectionFactory.empty();
        MockConnectionFactoryProvider.setSupportsAlways();
        MockConnectionFactoryProvider.setCreateCallbackReturn(connectionFactory);

        Set<String> listenerClasses = Collections.singleton(TestProxyExecutionListener.class.getName());
        this.provider.create(ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "foo")
            .option(PROXY_LISTENERS, listenerClasses)
            .build());

        assertThat(TestProxyExecutionListener.isInstantiated())
            .as("Listener should be instantiated")
            .isTrue();
    }

    @Test
    void invalidProtocol() {

        ConnectionFactoryOptions emptyProtocolOptions = ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "")  // empty name
            .build();

        assertThatThrownBy(() -> this.provider.create(emptyProtocolOptions))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Protocol  is not valid.");

        ConnectionFactoryOptions whiteSpaceProtocolOptions = ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "  ")  // name with spaces
            .build();

        assertThatThrownBy(() -> this.provider.create(whiteSpaceProtocolOptions))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Protocol    is not valid.");

        ConnectionFactoryOptions noProtocolOptions = ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .build();

        assertThatThrownBy(() -> this.provider.create(noProtocolOptions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No value found for protocol");

    }

    @Test
    void invalidListenerClassName() {

        MockConnectionFactoryProvider.setSupportsAlways();
        MockConnectionFactoryProvider.setCreateCallbackReturn(MockConnectionFactory.empty());

        ConnectionFactoryOptions invalidListenerClassNameOptions = ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "foo")
            .option(PROXY_LISTENERS, Collections.singleton("invalid.class"))
            .build();

        assertThatThrownBy(() -> this.provider.create(invalidListenerClassNameOptions))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid.class is not a valid proxy listener class");

    }

    @Test
    void invalidListenerClass() {

        MockConnectionFactoryProvider.setSupportsAlways();
        MockConnectionFactoryProvider.setCreateCallbackReturn(MockConnectionFactory.empty());

        String invalidListenerClassName = InvalidTestProxyExecutionListener.class.getName();
        ConnectionFactoryOptions invalidListenerClassNameOptions = ConnectionFactoryOptions.builder()
            .option(DRIVER, "proxy")
            .option(PROTOCOL, "foo")
            .option(PROXY_LISTENERS, Collections.singleton(invalidListenerClassName))
            .build();

        assertThatThrownBy(() -> this.provider.create(invalidListenerClassNameOptions))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(invalidListenerClassName + " is not a proxy listener class");

    }

    static class TestProxyExecutionListener implements ProxyExecutionListener {

        static boolean instantiated = false;

        public TestProxyExecutionListener() {
            instantiated = true;
        }

        static boolean isInstantiated() {
            return instantiated;
        }

        static void reset() {
            instantiated = false;
        }

    }

    /**
     * Does NOT implement {@link ProxyExecutionListener}.
     */
    static class InvalidTestProxyExecutionListener {

    }

}
