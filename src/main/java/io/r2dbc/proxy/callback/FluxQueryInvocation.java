/*
 * Copyright 2020 the original author or authors.
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

import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.util.annotation.Nullable;

/**
 * Call before/after query callbacks by wrapping the result of {@link Statement#execute()} or
 * {@link Batch#execute()} operations.
 *
 * @author Tadaya Tsuyukubo
 */
public class FluxQueryInvocation extends FluxOperator<Result, Result> {

    private final MutableQueryExecutionInfo executionInfo;

    private final ProxyConfig proxyConfig;

    public FluxQueryInvocation(Flux<? extends Result> source, MutableQueryExecutionInfo executionInfo, ProxyConfig proxyConfig) {
        super(source);
        this.executionInfo = executionInfo;
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Result> actual) {
        this.source.subscribe(new QueryInvocationSubscriber(actual, this.executionInfo, this.proxyConfig));
    }

    static class QueryInvocationSubscriber implements CoreSubscriber<Result>, Subscription, Scannable {

        private final CoreSubscriber<? super Result> delegate;

        private final MutableQueryExecutionInfo executionInfo;

        private final ProxyExecutionListener listener;

        private final CallbackHandlerSupport.StopWatch stopWatch;

        private Subscription subscription;

        public QueryInvocationSubscriber(CoreSubscriber<? super Result> delegate, MutableQueryExecutionInfo executionInfo, ProxyConfig proxyConfig) {
            this.delegate = delegate;
            this.executionInfo = executionInfo;
            this.listener = proxyConfig.getListeners();
            this.stopWatch = new CallbackHandlerSupport.StopWatch(proxyConfig.getClock());
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            beforeQuery();
            this.delegate.onSubscribe(this);
        }

        @Override
        public void onNext(Result result) {
            // When at least one element is emitted, consider query execution is success, even when
            // the publisher is canceled. see https://github.com/r2dbc/r2dbc-proxy/issues/55
            this.executionInfo.setSuccess(true);
            this.delegate.onNext(result);
        }

        @Override
        public void onError(Throwable t) {
            this.executionInfo.setThrowable(t);
            this.executionInfo.setSuccess(false);
            afterQuery();
            this.delegate.onError(t);
        }

        @Override
        public void onComplete() {
            this.executionInfo.setSuccess(true);
            afterQuery();
            this.delegate.onComplete();
        }

        @Override
        public void request(long n) {
            this.subscription.request(n);
        }

        @Override
        public void cancel() {
            // do not determine success/failure by cancel
            afterQuery();
            this.subscription.cancel();
        }

        @Override
        @Nullable
        @SuppressWarnings("rawtypes")
        public Object scanUnsafe(Attr key) {
            if (key == Attr.ACTUAL) {
                return this.delegate;
            }
            if (key == Attr.PARENT) {
                return this.subscription;
            }
            return null;
        }

        private void beforeQuery() {
            this.executionInfo.setThreadName(Thread.currentThread().getName());
            this.executionInfo.setThreadId(Thread.currentThread().getId());
            this.executionInfo.setCurrentMappedResult(null);
            this.executionInfo.setProxyEventType(ProxyEventType.BEFORE_QUERY);

            this.stopWatch.start();

            this.listener.beforeQuery(this.executionInfo);
        }

        private void afterQuery() {
            this.executionInfo.setExecuteDuration(this.stopWatch.getElapsedDuration());
            this.executionInfo.setThreadName(Thread.currentThread().getName());
            this.executionInfo.setThreadId(Thread.currentThread().getId());
            this.executionInfo.setCurrentMappedResult(null);
            this.executionInfo.setProxyEventType(ProxyEventType.AFTER_QUERY);

            this.listener.afterQuery(this.executionInfo);
        }
    }

}
