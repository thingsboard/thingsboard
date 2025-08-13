/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.cassandra;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.cassandra.guava.GuavaSessionBuilder;
import org.thingsboard.server.dao.cassandra.guava.GuavaSessionUtils;

import java.nio.file.Paths;

@Slf4j
public abstract class AbstractCassandraCluster {

    @Value("${cassandra.jmx}")
    private Boolean jmx;
    @Value("${cassandra.metrics}")
    private Boolean metrics;
    @Value("${cassandra.local_datacenter:datacenter1}")
    private String localDatacenter;

    @Value("${cassandra.cloud.secure_connect_bundle_path:}")
    private String cloudSecureConnectBundlePath;
    @Value("${cassandra.cloud.client_id:}")
    private String cloudClientId;
    @Value("${cassandra.cloud.client_secret:}")
    private String cloudClientSecret;

    @Autowired
    private CassandraDriverOptions driverOptions;

    @Autowired
    private Environment environment;

    private GuavaSessionBuilder sessionBuilder;

    private GuavaSession session;

    private JmxReporter reporter;

    private String keyspaceName;

    protected void init(String keyspaceName) {
        this.keyspaceName = keyspaceName;
        this.sessionBuilder = GuavaSessionUtils.builder().withConfigLoader(this.driverOptions.getLoader());
        if (!isInstall()) {
            initSession();
        }
    }

    public GuavaSession getSession() {
        if (!isInstall()) {
            return session;
        } else {
            if (session == null) {
                initSession();
            }
            return session;
        }
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    private void initSession() {
        if (this.keyspaceName != null) {
            this.sessionBuilder.withKeyspace(this.keyspaceName);
        }
        this.sessionBuilder.withLocalDatacenter(localDatacenter);

        if (StringUtils.isNotBlank(cloudSecureConnectBundlePath)) {
            this.sessionBuilder.withCloudSecureConnectBundle(Paths.get(cloudSecureConnectBundlePath));
            this.sessionBuilder.withAuthCredentials(cloudClientId, cloudClientSecret);
        }

        session = sessionBuilder.build();

        if (this.metrics && this.jmx) {
            MetricRegistry registry =
                    session.getMetrics().orElseThrow(
                            () -> new IllegalStateException("Metrics are disabled"))
                    .getRegistry();
            this.reporter =
                    JmxReporter.forRegistry(registry)
                            .inDomain("com.datastax.oss.driver")
                            .build();
            this.reporter.start();
        }
    }

    @PreDestroy
    public void close() {
        if (reporter != null) {
            reporter.stop();
        }
        if (session != null) {
            session.close();
        }
    }

    public ConsistencyLevel getDefaultReadConsistencyLevel() {
        return driverOptions.getDefaultReadConsistencyLevel();
    }

    public ConsistencyLevel getDefaultWriteConsistencyLevel() {
        return driverOptions.getDefaultWriteConsistencyLevel();
    }

}
