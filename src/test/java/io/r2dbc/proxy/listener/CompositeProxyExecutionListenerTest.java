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

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.test.MockMethodExecutionInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class CompositeProxyExecutionListenerTest {

    private LastExecutionAwareListener listener1;

    private LastExecutionAwareListener listener2;

    private CompositeProxyExecutionListener compositeListener;

    @BeforeEach
    void setUp() {
        this.listener1 = new LastExecutionAwareListener();
        this.listener2 = new LastExecutionAwareListener();

        this.compositeListener = new CompositeProxyExecutionListener(this.listener1, this.listener2);
    }

    @Test
    void beforeMethod() {

        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.builder()
            .proxyEventType(ProxyEventType.BEFORE_METHOD)
            .build();

        this.compositeListener.beforeMethod(executionInfo);

        assertThat(this.listener1.getBeforeMethodExecutionInfo()).isSameAs(executionInfo);
        assertThat(this.listener2.getBeforeMethodExecutionInfo()).isSameAs(executionInfo);

    }

    @Test
    void afterMethod() {

        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.builder()
            .proxyEventType(ProxyEventType.AFTER_METHOD)
            .build();

        this.compositeListener.afterMethod(executionInfo);

        assertThat(this.listener1.getAfterMethodExecutionInfo()).isSameAs(executionInfo);
        assertThat(this.listener2.getAfterMethodExecutionInfo()).isSameAs(executionInfo);

    }

    @Test
    void beforeQuery() {

        QueryExecutionInfo executionInfo = MockQueryExecutionInfo.empty();

        this.compositeListener.beforeQuery(executionInfo);

        assertThat(this.listener1.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(this.listener2.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
    }

    @Test
    void afterQuery() {

        QueryExecutionInfo executionInfo = MockQueryExecutionInfo.empty();

        this.compositeListener.afterQuery(executionInfo);

        assertThat(this.listener1.getAfterQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(this.listener2.getAfterQueryExecutionInfo()).isSameAs(executionInfo);

    }

    @Test
    void eachQueryResult() {

        QueryExecutionInfo executionInfo = MockQueryExecutionInfo.empty();

        this.compositeListener.eachQueryResult(executionInfo);

        assertThat(this.listener1.getEachQueryResultExecutionInfo()).isSameAs(executionInfo);
        assertThat(this.listener2.getEachQueryResultExecutionInfo()).isSameAs(executionInfo);

    }


}
