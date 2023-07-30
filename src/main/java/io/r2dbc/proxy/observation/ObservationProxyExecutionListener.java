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
import io.r2dbc.proxy.callback.DelegatingContextView;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Create Micrometer Observations.
 *
 * @author Marcin Grzejszczak
 * @author Tadaya Tsuyukubo
 * @since 1.1.0
 */
public class ObservationProxyExecutionListener implements ProxyExecutionListener {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(ObservationProxyExecutionListener.class);

    private final ConnectionFactory connectionFactory;

    private final ObservationRegistry observationRegistry;

    @Nullable
    private final String remoteServiceAddress;

    /**
     * Whether to tag query parameter values.
     */
    private boolean includeParameterValues;

    /**
     * Build tag value for query parameters.
     */
    private QueryParametersTagProvider queryParametersTagProvider = new DefaultQueryParametersTagProvider();

    private QueryObservationConvention queryObservationConvention = new QueryObservationConvention() {

    };

    /**
     * Constructor that takes a connection url.
     *
     * @param observationRegistry observation registry
     * @param connectionFactory   connection factory
     * @param connectionUrl       connection url string (optional)
     */
    public ObservationProxyExecutionListener(ObservationRegistry observationRegistry,
                                             ConnectionFactory connectionFactory, @Nullable String connectionUrl) {
        this.observationRegistry = observationRegistry;
        this.connectionFactory = connectionFactory;
        if (connectionUrl != null) {
            this.remoteServiceAddress = parseR2dbcConnectionUrl(connectionUrl);
        } else {
            this.remoteServiceAddress = null;
        }
    }

    /**
     * Constructor that takes host and port.
     *
     * @param observationRegistry observation registry
     * @param connectionFactory   connection factory
     * @param host                host
     * @param port                port number (optional)
     * @since 1.1.2
     */
    public ObservationProxyExecutionListener(ObservationRegistry observationRegistry,
                                             ConnectionFactory connectionFactory, String host, @Nullable Integer port) {
        this.observationRegistry = observationRegistry;
        this.connectionFactory = connectionFactory;
        this.remoteServiceAddress = buildRemoteServiceAddress(host, port);
    }

    @Nullable
    private String parseR2dbcConnectionUrl(String connectionUrl) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(connectionUrl);
        String host = (String) options.getValue(ConnectionFactoryOptions.HOST);
        Integer portNumber = (Integer) options.getValue(ConnectionFactoryOptions.PORT);
        return buildRemoteServiceAddress(host, portNumber);
    }

    @Nullable
    private String buildRemoteServiceAddress(@Nullable String host, @Nullable Integer portNumber) {
        int port = portNumber != null ? portNumber : -1;
        try {
            URI uri = new URI(null, null, host, port, null, null, null);
            return uri.toString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    @Override
    public void beforeQuery(QueryExecutionInfo executionInfo) {
        if (this.observationRegistry.isNoop()) {
            return;
        }
        Observation parentObservation = executionInfo.getValueStore()
            .getOrDefault(ContextView.class, (ContextView) new DelegatingContextView(Context.empty()))
            .getOrDefault(ObservationThreadLocalAccessor.KEY, this.observationRegistry.getCurrentObservation());
        if (parentObservation == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent observation not present.");
            }
        }
        String name = this.connectionFactory.getMetadata().getName();
        Observation observation = clientObservation(parentObservation, executionInfo, name);
        if (log.isDebugEnabled()) {
            log.debug("Created a new child observation before query [" + observation + "]");
        }
        tagQueries(executionInfo, (QueryContext) observation.getContext());
        executionInfo.getValueStore().put(Observation.class, observation);
    }

    private Observation clientObservation(@Nullable Observation parentObservation, QueryExecutionInfo executionInfo, String name) {
        QueryContext context = new QueryContext();
        context.setRemoteServiceName(name);
        context.setRemoteServiceAddress(this.remoteServiceAddress);
        context.setConnectionName(name);
        context.setThreadName(executionInfo.getThreadName());
        Observation observation = R2dbcObservationDocumentation.R2DBC_QUERY_OBSERVATION.observation(this.observationRegistry, () -> context)
            .observationConvention(this.queryObservationConvention)
            .parentObservation(parentObservation);
        return observation.start();
    }

    private void tagQueries(QueryExecutionInfo executionInfo, QueryContext context) {
        int i = 0;
        for (QueryInfo queryInfo : executionInfo.getQueries()) {
            context.getQueries().add(queryInfo.getQuery());
            if (this.includeParameterValues) {
                String params = this.queryParametersTagProvider.getTagValue(queryInfo.getBindingsList());
                context.getParams().add(params);
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
            observation.event(R2dbcObservationDocumentation.Events.QUERY_RESULT);
        }
    }

    public void setIncludeParameterValues(boolean includeParameterValues) {
        this.includeParameterValues = includeParameterValues;
    }

    public void setQueryParametersTagProvider(QueryParametersTagProvider queryParametersTagProvider) {
        this.queryParametersTagProvider = queryParametersTagProvider;
    }

    public void setQueryObservationConvention(QueryObservationConvention queryObservationConvention) {
        this.queryObservationConvention = queryObservationConvention;
    }
}
