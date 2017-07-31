/**
 * Copyright © 2016-2017 The Thingsboard Authors
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


import com.datastax.driver.core.*;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Data
public abstract class AbstractCassandraCluster {

    private static final String COMMA = ",";
    private static final String COLON = ":";

    @Value("${cassandra.cluster_name}")
    private String clusterName;
    @Value("${cassandra.url}")
    private String url;
    @Value("${cassandra.compression}")
    private String compression;
    @Value("${cassandra.ssl}")
    private Boolean ssl;
    @Value("${cassandra.jmx}")
    private Boolean jmx;
    @Value("${cassandra.metrics}")
    private Boolean metrics;
    @Value("${cassandra.credentials}")
    private Boolean credentials;
    @Value("${cassandra.username}")
    private String username;
    @Value("${cassandra.password}")
    private String password;
    @Value("${cassandra.init_timeout_ms}")
    private long initTimeout;
    @Value("${cassandra.init_retry_interval_ms}")
    private long initRetryInterval;

    @Autowired
    private CassandraSocketOptions socketOpts;

    @Autowired
    private CassandraQueryOptions queryOpts;

    @Autowired
    private Environment environment;

    private Cluster cluster;

    @Getter(AccessLevel.NONE) private Session session;

    private MappingManager mappingManager;

    public <T> Mapper<T> getMapper(Class<T> clazz) {
        return mappingManager.mapper(clazz);
    }

    private String keyspaceName;

    protected void init(String keyspaceName) {
        this.keyspaceName = keyspaceName;
        Cluster.Builder builder = Cluster.builder()
                .addContactPointsWithPorts(getContactPoints(url))
                .withClusterName(clusterName)
                .withSocketOptions(socketOpts.getOpts())
                .withPoolingOptions(new PoolingOptions()
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, 32768));
        builder.withQueryOptions(queryOpts.getOpts());
        builder.withCompression(StringUtils.isEmpty(compression) ? Compression.NONE : Compression.valueOf(compression.toUpperCase()));
        if (ssl) {
            builder.withSSL();
        }
        if (!jmx) {
            builder.withoutJMXReporting();
        }
        if (!metrics) {
            builder.withoutMetrics();
        }
        if (credentials) {
            builder.withCredentials(username, password);
        }
        cluster = builder.build();
        cluster.init();
        if (!isInstall()) {
            initSession();
        }
    }

    public Session getSession() {
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
        return environment.acceptsProfiles("install");
    }

    private void initSession() {
        long endTime = System.currentTimeMillis() + initTimeout;
        while (System.currentTimeMillis() < endTime) {
            try {

                if (this.keyspaceName != null) {
                    session = cluster.connect(keyspaceName);
                } else {
                    session = cluster.connect();
                }
                mappingManager = new MappingManager(session);
                break;
            } catch (Exception e) {
                log.warn("Failed to initialize cassandra cluster due to {}. Will retry in {} ms", e.getMessage(), initRetryInterval);
                try {
                    Thread.sleep(initRetryInterval);
                } catch (InterruptedException ie) {
                    log.warn("Failed to wait until retry", ie);
                }
            }
        }
    }

    @PreDestroy
    public void close() {
        if (cluster != null) {
            cluster.close();
        }
    }

    private List<InetSocketAddress> getContactPoints(String url) {
        List<InetSocketAddress> result;
        if (StringUtils.isBlank(url)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : url.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                Integer port = Integer.valueOf(hostPort.split(COLON)[1]);
                result.add(new InetSocketAddress(host, port));
            }
        }
        return result;
    }

    public ConsistencyLevel getDefaultReadConsistencyLevel() {
        return queryOpts.getDefaultReadConsistencyLevel();
    }

    public ConsistencyLevel getDefaultWriteConsistencyLevel() {
        return queryOpts.getDefaultWriteConsistencyLevel();
    }

}
