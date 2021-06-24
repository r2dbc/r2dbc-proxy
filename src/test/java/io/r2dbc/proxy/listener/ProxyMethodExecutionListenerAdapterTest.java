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

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.test.MockMethodExecutionInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.invocation.Invocation;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Test for {@link ProxyMethodExecutionListenerAdapter}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyMethodExecutionListenerAdapterTest {

    /**
     * Test to verify invocation on delegated {@link ProxyMethodExecutionListener} by calling
     * {@link ProxyMethodExecutionListenerAdapter#beforeMethod(MethodExecutionInfo)} and
     * {@link ProxyMethodExecutionListenerAdapter#afterMethod(MethodExecutionInfo)}.
     *
     * @param clazz class that r2dbc-proxy creates a proxy
     */
    @ParameterizedTest
    @ProxyClassesSource
    void methodInvocations(Class<?> clazz) {
        String className = clazz.getSimpleName();
        Method[] declaredMethods = clazz.getMethods();
        for (Method methodToInvoke : declaredMethods) {
            if (methodToInvoke.isSynthetic()) {
                continue;
            }

            String methodName = methodToInvoke.getName();

            // beforeXxxOnYyy : Xxx is a capitalized method-name and Yyy is a capitalized class-name
            String expectedBeforeMethodName = "before" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
            String expectedAfterMethodName = "after" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);

            // mock executing method
            MethodExecutionInfo methodExecutionInfo = MockMethodExecutionInfo.builder()
                .target(mock(clazz))
                .method(methodToInvoke)
                .build();

            ProxyMethodExecutionListener methodListener = mock(ProxyMethodExecutionListener.class);
            ProxyExecutionListener listener = new ProxyMethodExecutionListenerAdapter(methodListener);

            // invoke beforeMethod()
            listener.beforeMethod(methodExecutionInfo);

            // ProxyMethodExecutionListenerAdapter#beforeMethod calls its delegate in this order:
            // - "beforeMethod"
            // - "beforeXxxOnYyy"
            assertThat(mockingDetails(methodListener).getInvocations())
                .hasSize(2)
                .extracting(Invocation::getMethod)
                .extracting(Method::getName)
                .containsExactly("beforeMethod", expectedBeforeMethodName);

            // reset
            clearInvocations(methodListener);

            listener.afterMethod(methodExecutionInfo);

            // ProxyMethodExecutionListenerAdapter#afterMethod calls its delegate in this order:
            // - "afterXxxOnYyy"
            // - "afterMethod"
            assertThat(mockingDetails(methodListener).getInvocations())
                .hasSize(2)
                .extracting(Invocation::getMethod)
                .extracting(Method::getName)
                .containsExactly(expectedAfterMethodName, "afterMethod");
        }

    }

    @Test
    void queryExecution() {
        ProxyMethodExecutionListener methodListener = mock(ProxyMethodExecutionListener.class);
        ProxyExecutionListener listener = new ProxyMethodExecutionListenerAdapter(methodListener);

        QueryExecutionInfo queryExecutionInfo;

        // for Statement#execute
        queryExecutionInfo = MockQueryExecutionInfo.builder()
            .type(ExecutionType.STATEMENT)
            .build();

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(methodListener, "beforeQuery", "beforeExecuteOnStatement");

        clearInvocations(methodListener);

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(methodListener, "afterExecuteOnStatement", "afterQuery");

        clearInvocations(methodListener);


        // for Batch#execute
        queryExecutionInfo = MockQueryExecutionInfo.builder()
            .type(ExecutionType.BATCH)
            .build();

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(methodListener, "beforeQuery", "beforeExecuteOnBatch");

        clearInvocations(methodListener);

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(methodListener, "afterExecuteOnBatch", "afterQuery");
    }

    private void verifyQueryExecutionInvocation(ProxyMethodExecutionListener mockListener, String... expectedMethodNames) {
        assertThat(mockingDetails(mockListener).getInvocations())
            .hasSize(2)
            .extracting(Invocation::getMethod)
            .extracting(Method::getName)
            .containsExactly(expectedMethodNames);
    }


    @Test
    void methodInvocationWithConcreteClass() {
        // just declare an abstract class to get Method object
        abstract class MyConnectionFactory implements ConnectionFactory {

            @Override
            public ConnectionFactoryMetadata getMetadata() {
                return null;
            }
        }

        ProxyMethodExecutionListener methodListener = mock(ProxyMethodExecutionListener.class);
        ProxyExecutionListener listener = new ProxyMethodExecutionListenerAdapter(methodListener);

        Method getMetadataMethod = ReflectionUtils.findMethod(MyConnectionFactory.class, "getMetadata");

        // just make sure that retrieved method is not the one defined on interface
        Method getMetadataMethodFromInterface = ReflectionUtils.findMethod(ConnectionFactory.class, "getMetadata");
        assertThat(getMetadataMethod).isNotEqualTo(getMetadataMethodFromInterface);

        MethodExecutionInfo methodExecutionInfo = MockMethodExecutionInfo.builder()
            .target(mock(MyConnectionFactory.class))
            .method(getMetadataMethod)
            .build();

        // test beforeQuery
        listener.beforeMethod(methodExecutionInfo);

        assertThat(mockingDetails(methodListener).getInvocations())
            .extracting(Invocation::getMethod)
            .extracting(Method::getName)
            .contains("beforeGetMetadataOnConnectionFactory");
    }


    // TODO: add test for onEachQueryResult

}
