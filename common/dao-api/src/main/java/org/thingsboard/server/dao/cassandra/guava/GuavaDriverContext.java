/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PrepareRequest;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeStateListener;
import com.datastax.oss.driver.api.core.metadata.schema.SchemaChangeListener;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.api.core.tracker.RequestTracker;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareSyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestSyncProcessor;
import com.datastax.oss.driver.internal.core.session.RequestProcessorRegistry;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A Custom {@link DefaultDriverContext} that overrides {@link #getRequestProcessorRegistry()} to
 * return a {@link RequestProcessorRegistry} that includes processors for returning guava futures.
 */
public class GuavaDriverContext extends DefaultDriverContext {

    public GuavaDriverContext(
            DriverConfigLoader configLoader,
            List<TypeCodec<?>> typeCodecs,
            NodeStateListener nodeStateListener,
            SchemaChangeListener schemaChangeListener,
            RequestTracker requestTracker,
            Map<String, String> localDatacenters,
            Map<String, Predicate<Node>> nodeFilters,
            ClassLoader classLoader) {
        super(
                configLoader,
                ProgrammaticArguments.builder()
                        .addTypeCodecs(typeCodecs.toArray(new TypeCodec<?>[0]))
                        .withNodeStateListener(nodeStateListener)
                        .withSchemaChangeListener(schemaChangeListener)
                        .withRequestTracker(requestTracker)
                        .withLocalDatacenters(localDatacenters)
                        .withNodeFilters(nodeFilters)
                        .withClassLoader(classLoader)
                        .build());
    }

    @Override
    public RequestProcessorRegistry buildRequestProcessorRegistry() {
        // Register the typical request processors, except instead of the normal async processors,
        // use GuavaRequestAsyncProcessor to return ListenableFutures in async methods.

        CqlRequestAsyncProcessor cqlRequestAsyncProcessor = new CqlRequestAsyncProcessor();
        CqlPrepareAsyncProcessor cqlPrepareAsyncProcessor = new CqlPrepareAsyncProcessor();
        CqlRequestSyncProcessor cqlRequestSyncProcessor =
                new CqlRequestSyncProcessor(cqlRequestAsyncProcessor);

        return new RequestProcessorRegistry(
                getSessionName(),
                cqlRequestSyncProcessor,
                new CqlPrepareSyncProcessor(cqlPrepareAsyncProcessor),
                new GuavaRequestAsyncProcessor<>(
                        cqlRequestAsyncProcessor, Statement.class, GuavaSession.ASYNC),
                new GuavaRequestAsyncProcessor<>(
                        cqlPrepareAsyncProcessor, PrepareRequest.class, GuavaSession.ASYNC_PREPARED));
    }
}
