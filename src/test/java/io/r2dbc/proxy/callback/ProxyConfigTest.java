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

package io.r2dbc.proxy.callback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfigTest {

    @Test
    void setProxyFactoryFactory() {
        ProxyConfig proxyConfig = new ProxyConfig();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);

        when(proxyFactoryFactory.create(proxyConfig)).thenReturn(proxyFactory);

        proxyConfig.setProxyFactoryFactory(proxyFactoryFactory);

        assertThat(proxyConfig.getProxyFactory()).isSameAs(proxyFactory);

        assertThat(proxyConfig.getProxyFactory())
            .as("Second time calling getProxyFactory() should return same instance")
            .isSameAs(proxyFactory);
    }

}
