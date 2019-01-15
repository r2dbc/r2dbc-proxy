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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;

/**
 * Defines methods to augment execution of proxy methods used by child classes.
 *
 * @author Tadaya Tsuyukubo
 */
abstract class CallbackHandlerSupport implements CallbackHandler {

    private static final Set<Method> PASS_THROUGH_METHODS;

    static {
        try {
            Method objectToStringMethod = Object.class.getMethod("toString");
            PASS_THROUGH_METHODS = Arrays.stream(Object.class.getMethods())
                .filter(method -> !objectToStringMethod.equals(method))
                .collect(toSet());

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility class to get duration of executions.
     */
    private static class StopWatch {

        private Clock clock;

        private Instant startTime;

        private StopWatch(Clock clock) {
            this.clock = clock;
        }

        public StopWatch start() {
            this.startTime = this.clock.instant();
            return this;
        }

        public Duration getElapsedDuration() {
            return Duration.between(this.startTime, this.clock.instant());
        }

    }

    protected final ProxyConfig proxyConfig;


    public CallbackHandlerSupport(ProxyConfig proxyConfig) {
        this.proxyConfig = Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");
    }

    /**
     * Augment method invocation and call method listener.
     *
     * @param method         method to invoke on target
     * @param target         an object being invoked
     * @param args           arguments for the method
     * @param listener       listener that before/aftre method callbacks will be called
     * @param connectionInfo current connection information
     * @param onMap          a callback that will be chained on "map()" right after the result of the method invocation
     * @param onComplete     a callback that will be chained as the first doOnComplete on the result of the method invocation
     * @return result of invoking the original object
     * @throws Throwable                thrown exception during the invocation
     * @throws IllegalArgumentException if {@code method} is {@code null}
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code listener} is {@code null}
     */
    protected Object proceedExecution(Method method, Object target, Object[] args,
                                      ProxyExecutionListener listener, ConnectionInfo connectionInfo,
                                      BiFunction<Object, DefaultMethodExecutionInfo, Object> onMap,
                                      Consumer<MethodExecutionInfo> onComplete) throws Throwable {
        Assert.requireNonNull(method, "method must not be null");
        Assert.requireNonNull(target, "target must not be null");
        Assert.requireNonNull(listener, "listener must not be null");

        if (PASS_THROUGH_METHODS.contains(method)) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        // special handling for toString()
        if ("toString".equals(method.getName())) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());   // ConnectionFactory, Connection, Batch, or Statement
            sb.append("-proxy [");
            sb.append(target.toString());
            sb.append("]");
            return sb.toString(); // differentiate toString message.
        }


        StopWatch stopWatch = new StopWatch(this.proxyConfig.getClock());

        DefaultMethodExecutionInfo executionInfo = new DefaultMethodExecutionInfo();
        executionInfo.setMethod(method);
        executionInfo.setMethodArgs(args);
        executionInfo.setTarget(target);
        executionInfo.setConnectionInfo(connectionInfo);

        Class<?> returnType = method.getReturnType();

        if (Publisher.class.isAssignableFrom(returnType)) {

            Publisher<?> result;
            try {
                result = (Publisher<?>) method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }

            return Flux.empty()
                .doOnSubscribe(s -> {

                    executionInfo.setThreadName(Thread.currentThread().getName());
                    executionInfo.setThreadId(Thread.currentThread().getId());
                    executionInfo.setProxyEventType(ProxyEventType.BEFORE_METHOD);

                    listener.beforeMethod(executionInfo);

                    stopWatch.start();
                })
                .concatWith(result)
                .map(resultObj -> {

                    // set produced object as result
                    executionInfo.setResult(resultObj);

                    // apply a function to flux-chain right after the original publisher operations
                    if (onMap != null) {
                        return onMap.apply(resultObj, executionInfo);
                    }
                    return resultObj;
                })
                .doOnComplete(() -> {
                    // apply a consumer to flux-chain right after the original publisher operations
                    // this is the first chained doOnComplete on the result publisher
                    if (onComplete != null) {
                        onComplete.accept(executionInfo);
                    }
                })
                .doOnError(throwable -> {
                    executionInfo.setThrown(throwable);
                })
                .doFinally(signalType -> {

                    executionInfo.setExecuteDuration(stopWatch.getElapsedDuration());
                    executionInfo.setThreadName(Thread.currentThread().getName());
                    executionInfo.setThreadId(Thread.currentThread().getId());
                    executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);

                    listener.afterMethod(executionInfo);
                });


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
                result = method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                thrown = ex.getTargetException();
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
     * @param flux          query invocation result publisher
     * @param executionInfo query execution context info
     * @return query invocation result flux
     * @throws IllegalArgumentException if {@code flux} is {@code null}
     * @throws IllegalArgumentException if {@code executionInfo} is {@code null}
     */
    protected Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux, DefaultQueryExecutionInfo executionInfo) {
        Assert.requireNonNull(flux, "flux must not be null");
        Assert.requireNonNull(executionInfo, "executionInfo must not be null");

        ProxyExecutionListener listener = this.proxyConfig.getListeners();

        StopWatch stopWatch = new StopWatch(this.proxyConfig.getClock());

        Flux<? extends Result> queryExecutionFlux = Flux.empty()
            .ofType(Result.class)
            .doOnSubscribe(s -> {

                executionInfo.setThreadName(Thread.currentThread().getName());
                executionInfo.setThreadId(Thread.currentThread().getId());
                executionInfo.setCurrentMappedResult(null);
                executionInfo.setProxyEventType(ProxyEventType.BEFORE_QUERY);

                listener.beforeQuery(executionInfo);

                stopWatch.start();

            })
            .concatWith(flux)
            .doOnComplete(() -> {
                executionInfo.setSuccess(true);
            })
            .doOnError(throwable -> {
                executionInfo.setThrowable(throwable);
                executionInfo.setSuccess(false);
            })
            .doFinally(signalType -> {

                executionInfo.setExecuteDuration(stopWatch.getElapsedDuration());
                executionInfo.setThreadName(Thread.currentThread().getName());
                executionInfo.setThreadId(Thread.currentThread().getId());
                executionInfo.setCurrentMappedResult(null);
                executionInfo.setProxyEventType(ProxyEventType.AFTER_QUERY);

                listener.afterQuery(executionInfo);
            });

        ProxyFactory proxyFactory = this.proxyConfig.getProxyFactory();

        // return a publisher that returns proxy Result
        return Flux.from(queryExecutionFlux)
            .flatMap(queryResult -> Mono.just(proxyFactory.wrapResult(queryResult, executionInfo)));

    }

}
