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

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class LifeCycleExecutionListenerTest {

    /**
     * Test invoking {@link LifeCycleExecutionListener#beforeMethod(MethodExecutionInfo)} and
     * {@link LifeCycleExecutionListener#afterMethod(MethodExecutionInfo)} invokes corresponding
     * before/after methods defined on {@link LifeCycleListener}.
     *
     * @param clazz class that datasource-proxy-r2dbc creates proxy
     */
    @ParameterizedTest
    @ProxyClassesSource
    void methodInvocations(Class<?> clazz) {
        String className = clazz.getSimpleName();

        List<Method> invokedMethods = new ArrayList<>();
        LifeCycleListener lifeCycleListener = createLifeCycleListener(invokedMethods);
        LifeCycleExecutionListener listener = LifeCycleExecutionListener.of(lifeCycleListener);

        MethodExecutionInfo methodExecutionInfo = mock(MethodExecutionInfo.class);

        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method methodToInvoke : declaredMethods) {
            String methodName = methodToInvoke.getName();

            // beforeXxxOnYyy : Xxx is a capitalized method-name and Yyy is a capitalized class-name
            String expectedBeforeMethodName = "before" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
            String expectedAfterMethodName = "after" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);

            // mock executing method
            when(methodExecutionInfo.getMethod()).thenReturn(methodToInvoke);

            // invoke beforeMethod()
            listener.beforeMethod(methodExecutionInfo);

            // first method is beforeMethod
            // second method is beforeXxxOnYyy
            assertThat(invokedMethods)
                .hasSize(2)
                .extracting(Method::getName)
                .containsExactly("beforeMethod", expectedBeforeMethodName)
            ;

            // extra check for beforeXxxOnYyy
            Method beforeXxxOnYyy = invokedMethods.get(1);
            assertThat(beforeXxxOnYyy.getDeclaringClass()).isEqualTo(LifeCycleListener.class);

            // reset
            invokedMethods.clear();

            listener.afterMethod(methodExecutionInfo);

            // first method is afterXxxOnYyy
            // second method is afterMethod
            assertThat(invokedMethods)
                .hasSize(2)
                .extracting(Method::getName)
                .containsExactly(expectedAfterMethodName, "afterMethod")
            ;

            // extra check for afterXxxOnYyy
            Method afterXxxOnYyy = invokedMethods.get(0);
            assertThat(afterXxxOnYyy.getDeclaringClass()).isEqualTo(LifeCycleListener.class);

            // reset
            invokedMethods.clear();
            reset(methodExecutionInfo);
        }

    }

    @Test
    void queryExecution() {

        List<Method> invokedMethods = new ArrayList<>();
        LifeCycleListener lifeCycleListener = createLifeCycleListener(invokedMethods);
        LifeCycleExecutionListener listener = LifeCycleExecutionListener.of(lifeCycleListener);

        QueryExecutionInfo queryExecutionInfo;

        // for Statement#execute
        queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setType(ExecutionType.STATEMENT);

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "beforeQuery", "beforeExecuteOnStatement");

        invokedMethods.clear();

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "afterExecuteOnStatement", "afterQuery");

        assertThat(invokedMethods.get(0).getDeclaringClass()).isEqualTo(LifeCycleListener.class);
        assertThat(invokedMethods.get(1).getDeclaringClass()).isEqualTo(LifeCycleListener.class);

        invokedMethods.clear();


        // for Batch#execute
        queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setType(ExecutionType.BATCH);

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "beforeQuery", "beforeExecuteOnBatch");
        invokedMethods.clear();

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "afterExecuteOnBatch", "afterQuery");

    }

    private void verifyQueryExecutionInvocation(List<Method> invokedMethods, String... expectedMethodNames) {
        assertThat(invokedMethods)
            .hasSize(2)
            .extracting(Method::getName)
            .containsExactly(expectedMethodNames)
        ;

        assertThat(invokedMethods.get(0).getDeclaringClass()).isEqualTo(LifeCycleListener.class);
        assertThat(invokedMethods.get(1).getDeclaringClass()).isEqualTo(LifeCycleListener.class);

    }

    private LifeCycleListener createLifeCycleListener(List<Method> invokedMethods) {
        // use spring aop framework to create a proxy of LifeCycleListener that just keeps the
        // invoked methods
        MethodInterceptor interceptor = invocation -> {
            invokedMethods.add(invocation.getMethod());
            return null;  // don't proceed the proxy
        };

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvice(interceptor);
        proxyFactory.addInterface(LifeCycleListener.class);
        return (LifeCycleListener) proxyFactory.getProxy();
    }


    // TODO: add test for onEachQueryResult


}
