/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeStateListener;
import com.datastax.oss.driver.api.core.metadata.schema.SchemaChangeListener;
import com.datastax.oss.driver.api.core.session.SessionBuilder;
import com.datastax.oss.driver.api.core.tracker.RequestTracker;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class GuavaSessionBuilder extends SessionBuilder<GuavaSessionBuilder, GuavaSession> {

    @Override
    protected DriverContext buildContext(
            DriverConfigLoader configLoader,
            List<TypeCodec<?>> typeCodecs,
            NodeStateListener nodeStateListener,
            SchemaChangeListener schemaChangeListener,
            RequestTracker requestTracker,
            Map<String, String> localDatacenters,
            Map<String, Predicate<Node>> nodeFilters,
            ClassLoader classLoader) {
        return new GuavaDriverContext(
                configLoader,
                typeCodecs,
                nodeStateListener,
                schemaChangeListener,
                requestTracker,
                localDatacenters,
                nodeFilters,
                classLoader);
    }

    @Override
    protected GuavaSession wrap(@NonNull CqlSession defaultSession) {
        return new DefaultGuavaSession(defaultSession);
    }
}
