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
import reactor.util.context.ContextView;

/**
 * Custom subscriber/subscription to invoke query callback.
 *
 * @author Tadaya Tsuyukubo
 * @see CallbackHandlerSupport#interceptQueryExecution(Publisher, MutableQueryExecutionInfo)
 */
class QueryInvocationSubscriber implements CoreSubscriber<Result>, Subscription, Scannable, Fuseable.QueueSubscription<Result> {

    private final CoreSubscriber<? super Result> delegate;

    private final MutableQueryExecutionInfo executionInfo;

    private final ProxyExecutionListener listener;

    private final QueriesExecutionContext queriesExecutionContext;

    private final AfterQueryCallbackInvoker afterQueryCallbackInvoker;

    private Subscription subscription;

    private boolean resultProduced;

    public QueryInvocationSubscriber(CoreSubscriber<? super Result> delegate, MutableQueryExecutionInfo executionInfo, ProxyConfig proxyConfig, QueriesExecutionContext queriesExecutionContext) {
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
        beforeQuery();
        this.delegate.onSubscribe(this);
    }

    @Override
    public void onNext(Result result) {
        this.queriesExecutionContext.incrementProducedCount();
        this.resultProduced = true;
        this.executionInfo.setSuccess(true);
        this.delegate.onNext(result);
    }

    @Override
    public void onError(Throwable t) {
        this.executionInfo.setThrowable(t);
        this.executionInfo.setSuccess(false);

        // mark this publisher produced all Results
        this.queriesExecutionContext.markAllProduced();
        if (this.queriesExecutionContext.isQueryFinished()) {
            afterQuery();
        }

        this.delegate.onError(t);
    }

    @Override
    public void onComplete() {
        // mark this publisher produced all Results
        this.queriesExecutionContext.markAllProduced();
        if (this.queriesExecutionContext.isQueryFinished()) {
            this.executionInfo.setSuccess(true);
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
        this.queriesExecutionContext.markAllProduced();
        if (this.queriesExecutionContext.isQueryFinished()) {
            // When at least one element is emitted, consider query execution is success, even when
            // the publisher is canceled. see https://github.com/r2dbc/r2dbc-proxy/issues/55
            if (this.resultProduced) {
                this.executionInfo.setSuccess(true);
            }
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

    private void beforeQuery() {
        this.executionInfo.setThreadName(Thread.currentThread().getName());
        this.executionInfo.setThreadId(Thread.currentThread().getId());
        this.executionInfo.setCurrentMappedResult(null);
        this.executionInfo.setProxyEventType(ProxyEventType.BEFORE_QUERY);
        // register reactor context as read only
        this.executionInfo.getValueStore().put(ContextView.class, new DelegatingContextView(currentContext()));

        this.queriesExecutionContext.startStopwatch();

        this.listener.beforeQuery(this.executionInfo);
    }

}
