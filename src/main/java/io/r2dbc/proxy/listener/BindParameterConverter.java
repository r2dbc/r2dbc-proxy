/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.BindInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.spi.Statement;

/**
 * Callback for {@link io.r2dbc.spi.Connection#createStatement(String)} and bind operations({@code bind} and {@code bindNull}).
 *
 * @author Tadaya Tsuyukubo
 */
public interface BindParameterConverter {

    /**
     * Create default {@link BindParameterConverter}.
     *
     * @return a bindParameterConverter
     */
    static BindParameterConverter create() {
        return new BindParameterConverter() {

        };
    }

    /**
     * Convert the given query to another one at {@link io.r2dbc.spi.Connection#createStatement(String)}.
     *
     * <p>This method will be called when {@link io.r2dbc.spi.Connection#createStatement(String)} is called(before actually
     * performing), and the returned query will be used as its input.
     * Additionally, in this method, any information can be stored in {@link StatementInfo#getValueStore()}
     * and they will be available at {@link #onBind(BindInfo, Statement, BindOperation)}.
     *
     * <p>Typical usage would be parsing the parameters in query and convert it to the parameter placeholder that target
     * database supports. In addition, construct a map that contains parameter indexes by the place holder to avoid parsing
     * query again at {@link #onBind(BindInfo, Statement, BindOperation)}.
     *
     * <p>For example, convert a query that uses colon prefix for named parameters:
     *
     * "INSERT INTO names (first_name) VALUES (:FIRST_NAME)" to "INSERT INTO names (first_name) VALUES (?)",
     * and construct a map ":FIRST_NAME" = 0 (index).
     *
     * @param query original query
     * @param info  contextual information for {@link io.r2dbc.spi.Connection#createStatement(String)}.
     * @return converted query
     */
    default String onCreateStatement(String query, StatementInfo info) {
        return query;
    }


    /**
     * Callback method for bind operations({@code bind} and {@code bindNull}) on {@link Statement} before actually performing those methods.
     *
     * Implementation of this method can modify the actual behavior.
     * When calling the {@code defaultBinding.proceed()} performs the actual invocation of original bind operation, and returns proxy {@link Statement}.
     * To skip actual invocation of the original bind operation, simply returns {@code proxyStatement}.
     *
     * @param info           contextual information for {@code bind} and {@code bindNull}.
     * @param proxyStatement proxy {@link Statement}.
     * @param defaultBinding perform default bind operations and returns a result of proxy {@link Statement}
     */
    default void onBind(BindInfo info, Statement proxyStatement, BindOperation defaultBinding) {
        defaultBinding.proceed();  // just perform default behavior
    }

    /**
     * Represent bind operation({@code bind} and {@code bindNull}).
     */
    @FunctionalInterface
    interface BindOperation {

        /**
         * Perform the bind operation.
         *
         * @return result of bind operation which is a {@link Statement}.
         */
        Statement proceed();
    }

}
