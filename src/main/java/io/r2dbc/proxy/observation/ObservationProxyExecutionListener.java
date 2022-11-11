/*
 * Copyright 2022 the original author or authors.
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

package io.r2dbc.proxy.observation;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;
import io.r2dbc.proxy.callback.DelegatingContextView;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * @author Marcin Grzejszczak
 * @author Tadaya Tsuyukubo
 */
public class ObservationProxyExecutionListener implements ProxyExecutionListener {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(ObservationProxyExecutionListener.class);

    private final ConnectionFactory connectionFactory;

    private final ObservationRegistry observationRegistry;

    private final String url;

    /**
     * Whether to tag query parameter values.
     */
    private boolean includeParameterValues;

    /**
     * Build tag value for query parameters.
     */
    private QueryParametersTagProvider queryParametersTagProvider = new DefaultQueryParametersTagProvider();

    public ObservationProxyExecutionListener(ObservationRegistry observationRegistry,
                                             ConnectionFactory connectionFactory, String url) {
        this.observationRegistry = observationRegistry;
        this.connectionFactory = connectionFactory;
        this.url = url;
    }

    @Override
    public void beforeQuery(QueryExecutionInfo executionInfo) {
        if (this.observationRegistry.isNoop()) {
            return;
        }
        Observation parentObservation = executionInfo.getValueStore()
            .getOrDefault(ContextView.class, new DelegatingContextView(Context.empty()))
            .getOrDefault(ObservationThreadLocalAccessor.KEY, this.observationRegistry.getCurrentObservation());
        if (parentObservation == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent observation not present, won't do any instrumentation");
            }
            return;
        }
        String name = this.connectionFactory.getMetadata().getName();
        Observation observation = clientObservation(parentObservation, executionInfo, name);
        if (log.isDebugEnabled()) {
            log.debug("Created a new child observation before query [" + observation + "]");
        }
        tagQueries(executionInfo, observation);
        executionInfo.getValueStore().put(Observation.class, observation);
    }

    Observation clientObservation(Observation parentObservation, QueryExecutionInfo executionInfo, String name) {
        // @formatter:off
        SenderContext<?> context = new SenderContext<>((carrier, key, value) -> { }, Kind.CLIENT);
        context.setRemoteServiceName(name);
        context.setRemoteServiceAddress(this.url);
        Observation observation = R2DbcObservationDocumentation.R2DBC_QUERY_OBSERVATION.observation(this.observationRegistry, () -> context)
            .parentObservation(parentObservation)
            .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.CONNECTION.withValue(name))
            .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.THREAD.withValue(executionInfo.getThreadName()));
        // @formatter:on
        return observation.start();
    }

    private void tagQueries(QueryExecutionInfo executionInfo, Observation observation) {
        int i = 0;
        for (QueryInfo queryInfo : executionInfo.getQueries()) {
            observation.highCardinalityKeyValue(
                String.format(R2DbcObservationDocumentation.HighCardinalityKeys.QUERY.name(), i),
                queryInfo.getQuery());
            if (this.includeParameterValues) {
                String params = this.queryParametersTagProvider.getTagValue(queryInfo.getBindingsList());
                observation.highCardinalityKeyValue(
                    String.format(R2DbcObservationDocumentation.HighCardinalityKeys.QUERY_PARAMETERS.name(), i),
                    params);
            }
            i = i + 1;
        }
    }


    @Override
    public void afterQuery(QueryExecutionInfo executionInfo) {
        Observation observation = executionInfo.getValueStore().get(Observation.class, Observation.class);
        if (observation != null) {
            if (log.isDebugEnabled()) {
                log.debug("Continued the child observation in after query [" + observation + "]");
            }
            final Throwable throwable = executionInfo.getThrowable();
            if (throwable != null) {
                observation.error(throwable);
            }
            observation.stop();
        }
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo executionInfo) {
        Observation observation = executionInfo.getValueStore().get(Observation.class, Observation.class);
        if (observation != null) {
            if (log.isDebugEnabled()) {
                log.debug("Marking after query result for observation [" + observation + "]");
            }
            observation.event(R2DbcObservationDocumentation.Events.QUERY_RESULT);
        }
    }

    public void setIncludeParameterValues(boolean includeParameterValues) {
        this.includeParameterValues = includeParameterValues;
    }

    public void setQueryParametersTagProvider(QueryParametersTagProvider queryParametersTagProvider) {
        this.queryParametersTagProvider = queryParametersTagProvider;
    }

}
