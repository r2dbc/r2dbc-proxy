/*
 * Copyright 2018 the original author or authors.
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

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Connection;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Hold method execution related information.
 *
 * @author Tadaya Tsuyukubo
 */
public interface MethodExecutionInfo {

    /**
     * Get the invoked object.
     *
     * @return the proxy instance that the method was invoked on
     */
    Object getTarget();

    /**
     * Get the invoked {@code Method}.
     *
     * @return invoked method
     */
    Method getMethod();

    /**
     * Get the arguments of the invocation.
     *
     * This can be {@code null} when method is invoked with no argument.
     *
     * @return argument lists or {@code null} if the invoked method did not take any arguments
     */
    Object[] getMethodArgs();

    /**
     * Get the result of invocation.
     * For {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback, this returns {@code null}.
     *
     * @return result
     */
    Object getResult();

    /**
     * Get the thrown exception.
     * For {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback or when the invocation
     * did't throw any error, this returns {@code null}.
     *
     * @return thrown exception
     */
    Throwable getThrown();

    /**
     * Get the {@link ConnectionInfo}.
     * When invoked operation is not associated to the {@link Connection}, this returns {@code null}.
     *
     * @return connection info
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Get the duration of the method invocation.
     * For {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback, this returns {@code null}.
     *
     * @return execution duration
     */
    Duration getExecuteDuration();

    /**
     * Get the thread name.
     *
     * @return thread name
     */
    String getThreadName();

    /**
     * Get the thread ID.
     *
     * @return thread ID
     */
    long getThreadId();

    /**
     * Get the proxy event type.
     *
     * @return proxy event type; either {@link ProxyEventType#BEFORE_METHOD} or {@link ProxyEventType#AFTER_METHOD}
     */
    ProxyEventType getProxyEventType();

    /**
     * Retrieve {@link ValueStore} which is associated to the scope of before/after method execution.
     *
     * Mainly used for passing values between {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} and
     * {@link ProxyExecutionListener#afterMethod(MethodExecutionInfo)}.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
