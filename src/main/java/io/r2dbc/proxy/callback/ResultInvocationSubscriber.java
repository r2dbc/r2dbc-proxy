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
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Custom subscriber/subscription to on {@code Result#[map|getRowsUpdated]}..
 *
 * @author Tadaya Tsuyukubo
 * @see CallbackHandlerSupport#interceptQueryExecution(Publisher, MutableQueryExecutionInfo)
 */
class ResultInvocationSubscriber implements CoreSubscriber<Object>, Subscription, Scannable, Fuseable.QueueSubscription<Object> {

    private static final AtomicIntegerFieldUpdater<ResultInvocationSubscriber> RESULT_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(ResultInvocationSubscriber.class, "resultCount");

    private final CoreSubscriber<Object> delegate;

    private final MutableQueryExecutionInfo executionInfo;

    private final ProxyExecutionListener listener;

    private final QueriesExecutionContext queriesExecutionContext;

    private final AfterQueryCallbackInvoker afterQueryCallbackInvoker;

    /**
     * Accessed via {@link #RESULT_COUNT_INCREMENTER}.
     */
    private volatile int resultCount;

    private Subscription subscription;

    public ResultInvocationSubscriber(CoreSubscriber<Object> delegate, MutableQueryExecutionInfo executionInfo, ProxyConfig proxyConfig, QueriesExecutionContext queriesExecutionContext) {
        this.delegate = delegate;
        this.executionInfo = executionInfo;
        this.listener = proxyConfig.getListeners();
        this.queriesExecutionContext = queriesExecutionContext;
        this.afterQueryCallbackInvoker = new AfterQueryCallbackInvoker(this.executionInfo, this.queriesExecutionContext, this.listener);
    }

    @Override
    public Context currentContext() {
        return this.delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        this.delegate.onSubscribe(this);
    }

    @Override
    public void onNext(Object mappedResult) {
        eachQueryResult(mappedResult, null);
        this.delegate.onNext(mappedResult);
    }

    @Override
    public void onError(Throwable t) {
        eachQueryResult(null, t);

        this.queriesExecutionContext.incrementConsumedCount();
        if (this.queriesExecutionContext.isQueryFinished()) {
            afterQuery();
        }

        this.delegate.onError(t);
    }

    @Override
    public void onComplete() {
        this.queriesExecutionContext.incrementConsumedCount();
        if (this.queriesExecutionContext.isQueryFinished()) {
            afterQuery();
        }

        this.delegate.onComplete();
    }

    @Override
    public void request(long n) {
        this.subscription.request(n);
    }

    @Override
    public void cancel() {
        // do not determine success/failure by cancel
        this.queriesExecutionContext.incrementConsumedCount();
        if (this.queriesExecutionContext.isQueryFinished()) {
            afterQuery();
        }

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

    @Override
    public int requestFusion(int requestedMode) {
        return Fuseable.NONE;
    }

    @Override
    public Result poll() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void clear() {

    }

    private void afterQuery() {
        this.afterQueryCallbackInvoker.afterQuery();
    }

    private void eachQueryResult(@Nullable Object mappedResult, @Nullable Throwable throwable) {
        this.executionInfo.setProxyEventType(ProxyEventType.EACH_QUERY_RESULT);
        this.executionInfo.setThreadName(Thread.currentThread().getName());
        this.executionInfo.setThreadId(Thread.currentThread().getId());

        this.executionInfo.setCurrentResultCount(RESULT_COUNT_INCREMENTER.incrementAndGet(this));
        this.executionInfo.setCurrentMappedResult(mappedResult);
        if (throwable != null) {
            this.executionInfo.setThrowable(throwable);
            this.executionInfo.setSuccess(false);
        } else {
            this.executionInfo.setThrowable(null);
            this.executionInfo.setSuccess(true);
        }

        this.listener.eachQueryResult(this.executionInfo);
    }
}
