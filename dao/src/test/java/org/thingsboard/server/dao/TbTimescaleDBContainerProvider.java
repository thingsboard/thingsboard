/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.TimescaleDBContainerProvider;

/**
 * Extends the upstream {@link TimescaleDBContainerProvider} to disable the
 * timescaledb-tune entrypoint script via NO_TS_TUNE=true.
 *
 * Works around a shell bug in /docker-entrypoint-initdb.d/001_timescaledb_tune.sh
 * that crashes the container entrypoint on cgroup v2 hosts (including CI agents)
 * when the kernel reports the 64-bit max for memory.max.
 *
 * Activated by the jdbc:tc:tbtimescaledb:&lt;tag&gt;:///... URL prefix
 * registered via META-INF/services.
 */
public class TbTimescaleDBContainerProvider extends TimescaleDBContainerProvider {

    private static final String NAME = "tbtimescaledb";

    @Override
    public boolean supports(String databaseType) {
        return NAME.equals(databaseType);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        JdbcDatabaseContainer container = super.newInstance(tag);
        container.withEnv("NO_TS_TUNE", "true");
        return container;
    }
}
