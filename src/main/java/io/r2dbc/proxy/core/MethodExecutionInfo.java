/*
 * Copyright 2018-2020 the original author or authors.
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
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Holds information about a method invocation.
 *
 * @author Tadaya Tsuyukubo
 */
public interface MethodExecutionInfo {

    /**
     * Returns the object on which the method was invoked.
     *
     * @return the proxy instance that the method was invoked on
     */
    Object getTarget();

    /**
     * Returns the invoked {@code Method}.
     *
     * @return invoked method
     */
    Method getMethod();

    /**
     * Returns the arguments passed to the invocation.
     *
     * This value may be {@code null} when the method is invoked without any arguments.
     *
     * @return argument lists or {@code null} if the invoked method did not take any arguments
     */
    @Nullable
    Object[] getMethodArgs();

    /**
     * Returns the result of the invocation.
     * For {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback, this returns {@code null}.
     *
     * @return result
     */
    @Nullable
    Object getResult();

    /**
     * Returns the exception thrown by the invocation.
     * For the {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback, or when the invocation
     * did not throw any error, this returns {@code null}.
     *
     * @return thrown exception
     */
    @Nullable
    Throwable getThrown();

    /**
     * Returns the associated {@link ConnectionInfo}.
     * When the invoked operation is not associated with the {@link Connection}, this returns {@code null}.
     *
     * @return connection info
     */
    @Nullable
    ConnectionInfo getConnectionInfo();

    /**
     * Returns the duration of the method invocation.
     * For {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} callback, this returns {@link Duration#ZERO}.
     *
     * @return execution duration
     */
    Duration getExecuteDuration();

    /**
     * Returns the thread name.
     *
     * @return thread name
     */
    String getThreadName();

    /**
     * Returns the thread ID.
     *
     * @return thread ID
     */
    long getThreadId();

    /**
     * Returns the proxy event type.
     *
     * @return proxy event type; either {@link ProxyEventType#BEFORE_METHOD} or {@link ProxyEventType#AFTER_METHOD}
     */
    ProxyEventType getProxyEventType();

    /**
     * Returns the {@link ValueStore} associated with the scope of the before/after method execution.
     *
     * Mainly used for passing values between {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)} and
     * {@link ProxyExecutionListener#afterMethod(MethodExecutionInfo)}.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
