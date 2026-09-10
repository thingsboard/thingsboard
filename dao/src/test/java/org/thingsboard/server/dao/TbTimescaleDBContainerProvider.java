// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
