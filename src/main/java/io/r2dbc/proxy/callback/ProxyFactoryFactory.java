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

/**
 * Factory to create a {@link ProxyFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyFactoryFactory {

    /**
     * Create {@link ProxyFactory} with given {@link ProxyConfig}.
     *
     * @param proxyConfig proxy config
     * @return proxy factory
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     */
    ProxyFactory create(ProxyConfig proxyConfig);
}
