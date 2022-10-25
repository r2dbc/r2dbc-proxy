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

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

/**
 * Custom subscriber/subscription to invoke method callback.
 *
 * This also implements {@link Subscription} to handle cancel case, and
 * {@link Fuseable.QueueSubscription} to disable fusion.
 *
 * @author Tadaya Tsuyukubo
 */
class MethodInvocationSubscriber implements CoreSubscriber<Object>, Subscription, Scannable, Fuseable.QueueSubscription<Object> {

    protected final CoreSubscriber<Object> delegate;

    protected final MutableMethodExecutionInfo executionInfo;

    protected final ProxyExecutionListener listener;

    protected final StopWatch stopWatch;

    protected Subscription subscription;

    @Nullable
    protected Consumer<MethodExecutionInfo> onComplete;

    public MethodInvocationSubscriber(CoreSubscriber<Object> delegate, MutableMethodExecutionInfo executionInfo, ProxyConfig proxyConfig, @Nullable Consumer<MethodExecutionInfo> onComplete) {
        this.delegate = delegate;
        this.executionInfo = executionInfo;
        this.listener = proxyConfig.getListeners();
        this.stopWatch = new StopWatch(proxyConfig.getClock());
        this.onComplete = onComplete;
    }

    @Override
    public Context currentContext() {
        return this.delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        beforeMethod();
        this.delegate.onSubscribe(this);
    }

    @Override
    public void onNext(Object object) {
        this.executionInfo.setResult(object); // set produced object as result
        this.delegate.onNext(object);
    }

    @Override
    public void onError(Throwable t) {
        this.executionInfo.setThrown(t);
        afterMethod();
        this.delegate.onError(t);
    }

    @Override
    public void onComplete() {
        if (this.onComplete != null) {
            this.onComplete.accept(this.executionInfo);
        }
        afterMethod();
        this.delegate.onComplete();
    }

    @Override
    public void request(long n) {
        this.subscription.request(n);
    }

    @Override
    public void cancel() {
        afterMethod();
        this.subscription.cancel();
    }

    @Override
    @Nullable
    @SuppressWarnings("rawtypes")
    public Object scanUnsafe(Scannable.Attr key) {
        if (key == Scannable.Attr.ACTUAL) {
            return this.delegate;
        }
        if (key == Scannable.Attr.PARENT) {
            return this.subscription;
        }
        return null;
    }

    @Override
    public int requestFusion(int requestedMode) {
        return Fuseable.NONE;
    }

    @Nullable
    @Override
    public Object poll() {
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

    private void beforeMethod() {
        this.executionInfo.setThreadName(Thread.currentThread().getName());
        this.executionInfo.setThreadId(Thread.currentThread().getId());
        this.executionInfo.setProxyEventType(ProxyEventType.BEFORE_METHOD);

        // register reactor context as read only
        this.executionInfo.getValueStore().put(ContextView.class, new DelegatingContextView(currentContext()));

        this.stopWatch.start();

        this.listener.beforeMethod(this.executionInfo);
    }

    private void afterMethod() {
        this.executionInfo.setExecuteDuration(this.stopWatch.getElapsedDuration());
        this.executionInfo.setThreadName(Thread.currentThread().getName());
        this.executionInfo.setThreadId(Thread.currentThread().getId());
        this.executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);

        this.listener.afterMethod(this.executionInfo);
    }

}
