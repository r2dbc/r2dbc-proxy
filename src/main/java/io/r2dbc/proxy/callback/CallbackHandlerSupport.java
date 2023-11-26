/*
 * Copyright 2018-2023 the original author or authors.
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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Wrapped;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines methods to augment execution of proxy methods used by child classes.
 *
 * @author Tadaya Tsuyukubo
 */
abstract class CallbackHandlerSupport implements CallbackHandler {

    /**
     * Strategy to invoke the original instance(non-proxy) and retrieve result.
     */
    @FunctionalInterface
    public interface MethodInvocationStrategy {

        /**
         * Retrieve the actual result from original object.
         *
         * @param method invocation method
         * @param target invocation target instance
         * @param args   invocation arguments. {@code null} when invocation didn't take any arguments.
         * @return actual invocation result (not a proxy object)
         * @throws Throwable actual thrown exception
         */
        Object invoke(Method method, Object target, @Nullable Object[] args) throws Throwable;
    }

    protected static final MethodInvocationStrategy DEFAULT_INVOCATION_STRATEGY = (method, target, args) -> {
        // Perform reflective invocation on target instance.
        // When underlying method throws exception, "Method#invoke()" throws InvocationTargetException.
        // Since this strategy requires throwing originally thrown exception, catch-and-throw the original
        // exception.
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();  // throw actual exception
        }
        return result;
    };

    private static final Set<String> COMMON_METHODS = new HashSet<>(Arrays.asList(
        "toString", "equals", "hashCode",
        "unwrap",  // "Wrapped#unwrap"
        "getProxyConfig", //  "ProxyConfigHolder#getProxyConfig"
        "unwrapConnection"  // "ConnectionHolder#unwrapConnection"
    ));

    protected final ProxyConfig proxyConfig;

    protected MethodInvocationStrategy methodInvocationStrategy = DEFAULT_INVOCATION_STRATEGY;

    public CallbackHandlerSupport(ProxyConfig proxyConfig) {
        this.proxyConfig = Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");
    }

    protected boolean isCommonMethod(String methodName) {
        return COMMON_METHODS.contains(methodName);
    }

    @Nullable
    protected Object handleCommonMethod(String methodName, Object original, @Nullable Object[] args, @Nullable Connection originalConnection) {
        if ("toString".equals(methodName)) {
            StringBuilder sb = new StringBuilder();
            sb.append(original.getClass().getSimpleName());
            sb.append("-proxy [");
            sb.append(original);
            sb.append("]");
            return sb.toString(); // differentiate toString message.
        } else if ("equals".equals(methodName)) {
            // when target is a proxy, also compares the proxied object
            return original.equals(args[0]) || (args[0] instanceof Wrapped && args[0] instanceof ProxyConfigHolder && original.equals(((Wrapped<?>) args[0]).unwrap()));
        } else if ("hashCode".equals(methodName)) {
            return original.hashCode();
        } else if ("getProxyConfig".equals(methodName)) {
            return this.proxyConfig;  // "ProxyConfigHolder#getProxyConfig"
        } else if ("unwrapConnection".equals(methodName)) {
            return originalConnection;  // "ConnectionHolder#unwrapConnection"
        } else if ("unwrap".equals(methodName)) {
            if (args == null) {
                return original; // for no-arg "unwrap"
            } else {
                Class<?> targetClass = (Class<?>) args[0];
                if (original instanceof Wrapped) {
                    return ((Wrapped<?>) original).unwrap(targetClass);
                }
                if (targetClass.isInstance(original)) {
                    return original;  // "original instanceof targetClass"
                }
                return null;
            }
        }
        throw new IllegalStateException(methodName + " does not match to the common method names.");
    }

    /**
     * Augment method invocation and call method listener.
     *
     * @param method         method to invoke on target
     * @param target         an object being invoked
     * @param args           arguments for the method. {@code null} if the method doesn't take any arguments.
     * @param listener       listener that before/after method callbacks will be called
     * @param connectionInfo current connection information. {@code null} when invoked operation is not associated to the {@link Connection}.
     * @param onComplete     a callback that will be invoked at successful termination(onComplete) of the result publisher.
     * @return result of invoking the original object
     * @throws Throwable                thrown exception during the invocation
     * @throws IllegalArgumentException if {@code method} is {@code null}
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code listener} is {@code null}
     */
    protected Object proceedExecution(Method method, Object target, @Nullable Object[] args,
                                      ProxyExecutionListener listener, @Nullable ConnectionInfo connectionInfo,
                                      @Nullable Consumer<MethodExecutionInfo> onComplete) throws Throwable {
        Assert.requireNonNull(method, "method must not be null");
        Assert.requireNonNull(target, "target must not be null");
        Assert.requireNonNull(listener, "listener must not be null");

        StopWatch stopWatch = new StopWatch(this.proxyConfig.getClock());

        MutableMethodExecutionInfo executionInfo = new MutableMethodExecutionInfo();
        executionInfo.setMethod(method);
        executionInfo.setMethodArgs(args);
        executionInfo.setTarget(target);
        executionInfo.setConnectionInfo(connectionInfo);

        Class<?> returnType = method.getReturnType();

        if (Publisher.class.isAssignableFrom(returnType)) {
            Publisher<?> result = (Publisher<?>) this.methodInvocationStrategy.invoke(method, target, args);
            Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer =
                Operators.liftPublisher((publisher, subscriber) ->
                    new MethodInvocationSubscriber(subscriber, executionInfo, proxyConfig, onComplete));
            if (result instanceof Mono) {
                return ((Mono<?>) result).cast(Object.class).transform(transformer);
            } else {
                return Flux.from(result).cast(Object.class).transform(transformer);
            }
        } else {
            // for method that generates non-publisher, execution happens when it is invoked.

            executionInfo.setThreadName(Thread.currentThread().getName());
            executionInfo.setThreadId(Thread.currentThread().getId());
            executionInfo.setProxyEventType(ProxyEventType.BEFORE_METHOD);

            listener.beforeMethod(executionInfo);

            stopWatch.start();

            Object result = null;
            Throwable thrown = null;
            try {
                result = this.methodInvocationStrategy.invoke(method, target, args);
            } catch (Throwable ex) {
                thrown = ex;  // capture the exception
                throw thrown;
            } finally {
                executionInfo.setResult(result);
                executionInfo.setThrown(thrown);
                executionInfo.setExecuteDuration(stopWatch.getElapsedDuration());
                executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);

                listener.afterMethod(executionInfo);
            }
            return result;

        }

    }

    /**
     * Augment query execution result to hook up listener lifecycle.
     *
     * @param publisher     query invocation result publisher
     * @param executionInfo query execution context info
     * @return query invocation result flux
     * @throws IllegalArgumentException if {@code flux} is {@code null}
     * @throws IllegalArgumentException if {@code executionInfo} is {@code null}
     */
    protected Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> publisher, MutableQueryExecutionInfo executionInfo) {
        Assert.requireNonNull(publisher, "flux must not be null");
        Assert.requireNonNull(executionInfo, "executionInfo must not be null");

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(this.proxyConfig.getClock());
        ProxyFactory proxyFactory = this.proxyConfig.getProxyFactory();
        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer =
            Operators.liftPublisher((pub, subscriber) ->
                new QueryInvocationSubscriber(subscriber, executionInfo, proxyConfig, queriesExecutionContext));

        return Flux.from(publisher)
            .cast(Result.class)
            .transform(transformer)
            .map(queryResult -> proxyFactory.wrapResult(queryResult, executionInfo, queriesExecutionContext));
    }

    /**
     * Set {@link MethodInvocationStrategy} to invoke the original instance(non-proxy) and retrieve result.
     *
     * @param methodInvocationStrategy strategy for method invocation
     * @throws IllegalArgumentException if {@code methodInvocationStrategy} is {@code null}
     * @see MethodInvocationStrategy
     */
    public void setMethodInvocationStrategy(MethodInvocationStrategy methodInvocationStrategy) {
        this.methodInvocationStrategy = Assert.requireNonNull(methodInvocationStrategy, "methodInvocationStrategy must not be null");
    }

}
