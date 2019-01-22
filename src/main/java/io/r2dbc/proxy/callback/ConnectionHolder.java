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

import io.r2dbc.spi.Connection;

/**
 * Provide methods to retrieve original {@link Connection} from proxy object.
 *
 * @author Tadaya Tsuyukubo
 * @see Connection
 * @see ProxyUtils
 */
public interface ConnectionHolder {

    /**
     * Retrieve original {@link Connection}.
     *
     * @return original connection
     */
    Connection unwrapConnection();

}
