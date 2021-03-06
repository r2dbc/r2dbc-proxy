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
 *
 */

package io.r2dbc.proxy.listener;

import io.r2dbc.spi.Row;

import javax.annotation.Nullable;

/**
 * TODO: document
 *
 * @author Tadaya Tsuyukubo
 */
@FunctionalInterface
public interface ResultRowConverter {

    /**
     * Create the default {@link ResultRowConverter}.
     *
     * @return a bindParameterConverter
     */
    static ResultRowConverter create() {
        return (proxyRow, args, getOperation) -> getOperation.proceed();
    }

    /**
     * Callback method for {@code Row#get(...)} before performing the original {@code Row#get(...)} method.
     * <p>
     * Implementation of this method can modify the actual behavior. For example, the callback can modify arguments and
     * return value, determine whether to call the original method or alternative methods, etc.
     * <p>
     * To perform the original {@code Row#get(...)} method, invoke {@code getOperation.proceed()}.
     *
     * @param proxyRow     proxy {@link Row}.
     * @param args         arguments of the original {@code Row#get(...)} call
     * @param getOperation perform {@code Row#get(...)} operation and returns its result
     * @return return value from {@code Row#get(...)} operation
     */
    @Nullable
    Object onGet(Row proxyRow, Object[] args, GetOperation getOperation);

    //    default Object onGet(Row proxyRow, Object[] args, GetOperation getOperation) {
//        return getOperation.proceed();  // just perform default behavior
//    }

    /**
     * Represent {@code Row#get(...)} operation.
     */
    @FunctionalInterface
    interface GetOperation {

        /**
         * Perform the get operation.
         *
         * @return result of the {@code Row#get(...)} operation
         */
        @Nullable
        Object proceed();
    }

}
