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
 */

package io.r2dbc.proxy.core;

import io.r2dbc.spi.R2dbcException;
import reactor.util.annotation.Nullable;

/**
 * Generic r2dbc-proxy exception.
 *
 * @author Tadaya Tsuyukubo
 */
public class R2dbcProxyException extends R2dbcException {

    /**
     * Create a new {@link R2dbcProxyException}.
     */
    public R2dbcProxyException() {
    }

    /**
     * Create a new {@link R2dbcProxyException}.
     *
     * @param reason the reason for the error.
     */
    public R2dbcProxyException(@Nullable String reason) {
        super(reason);
    }

    /**
     * Create a new {@link R2dbcProxyException}.
     *
     * @param reason the reason for the error.
     * @param cause  the cause of the error.
     */
    public R2dbcProxyException(@Nullable String reason, @Nullable Throwable cause) {
        super(reason, cause);
    }
}
