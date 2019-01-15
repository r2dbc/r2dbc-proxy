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

import java.lang.reflect.Method;

/**
 * Callback logic for proxy invocation.
 *
 * The logic is separated from proxy handler to implementation of this interface
 * in order to be reused in different proxy mechanism (JDK dynamic proxy, cglib, etc).
 *
 * @author Tadaya Tsuyukubo
 */
interface CallbackHandler {

    /**
     * When proxy is invoked, actual implementation of the proxy handler delegates the
     * invocation to this method.
     *
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the method that has invoked on the proxy instance
     * @param args   an array of objects that has passed to the method invocation.
     *               this can be {@code null} when method is invoked with no argument.
     * @return result returned from the method invocation on the proxy instance
     * @throws Throwable                the exception thrown from the method invocation on the proxy instance.
     * @throws IllegalArgumentException if {@code proxy} is {@code null}
     * @throws IllegalArgumentException if {@code method} is {@code null}
     */
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

}
