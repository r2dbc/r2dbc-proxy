/*
 * Copyright 2021 the original author or authors.
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

import io.r2dbc.proxy.callback.RowCallbackHandlerTest;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.test.MockRow;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ResultRowConverter}.
 *
 * @author Tadaya Tsuyukubo
 * @see RowCallbackHandlerTest
 */
class ResultRowConverterTest {

    private static Method GET_BY_INDEX_METHOD = ReflectionUtils.findMethod(Row.class, "get", int.class);

    @Test
    void defaultConverter() {
        ResultRowConverter converter = ResultRowConverter.create();

        Method method = GET_BY_INDEX_METHOD;
        Object[] args = new Object[0];

        MockRow mockRow = MockRow.empty();
        ResultRowConverter.GetOperation getOperation = mock(ResultRowConverter.GetOperation.class);
        when(getOperation.proceed()).thenReturn("foo");

        Object result = converter.onGet(mockRow, method, args, getOperation);
        assertThat(result).isEqualTo("foo");

        verify(getOperation).proceed();
    }

}
