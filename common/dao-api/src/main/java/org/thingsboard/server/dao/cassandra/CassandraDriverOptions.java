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
package org.thingsboard.server.dao.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Configuration
@Data
@NoSqlAnyDao
public class CassandraDriverOptions {

    private static final String COMMA = ",";

    @Value("${cassandra.cluster_name}")
    private String clusterName;
    @Value("${cassandra.url}")
    private String url;

    @Value("${cassandra.socket.connect_timeout}")
    private int connectTimeoutMillis;
    @Value("${cassandra.socket.read_timeout}")
    private int readTimeoutMillis;
    @Value("${cassandra.socket.keep_alive}")
    private Boolean keepAlive;
    @Value("${cassandra.socket.reuse_address}")
    private Boolean reuseAddress;
    @Value("${cassandra.socket.so_linger}")
    private Integer soLinger;
    @Value("${cassandra.socket.tcp_no_delay}")
    private Boolean tcpNoDelay;
    @Value("${cassandra.socket.receive_buffer_size}")
    private Integer receiveBufferSize;
    @Value("${cassandra.socket.send_buffer_size}")
    private Integer sendBufferSize;

    @Value("${cassandra.max_requests_per_connection_local:32768}")
    private int max_requests_local;
    @Value("${cassandra.max_requests_per_connection_remote:32768}")
    private int max_requests_remote;

    @Value("${cassandra.query.default_fetch_size}")
    private Integer defaultFetchSize;
    @Value("${cassandra.query.read_consistency_level}")
    private String readConsistencyLevel;
    @Value("${cassandra.query.write_consistency_level}")
    private String writeConsistencyLevel;

    @Value("${cassandra.compression}")
    private String compression;
    
    @Value("${cassandra.ssl.enabled}")
    private Boolean ssl;
    @Value("${cassandra.ssl.key_store}")
    private String sslKeyStore;
    @Value("${cassandra.ssl.key_store_password}")
    private String sslKeyStorePassword;
    @Value("${cassandra.ssl.trust_store}")
    private String sslTrustStore;
    @Value("${cassandra.ssl.trust_store_password}")
    private String sslTrustStorePassword;
    @Value("${cassandra.ssl.hostname_validation}")
    private Boolean sslHostnameValidation;
    @Value("${cassandra.ssl.cipher_suites}")
    private List<String> sslCipherSuites;
    
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

    private DriverConfigLoader loader;

    private ConsistencyLevel defaultReadConsistencyLevel;
    private ConsistencyLevel defaultWriteConsistencyLevel;

    @PostConstruct
    public void initLoader() {
        ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder =
                DriverConfigLoader.programmaticBuilder();

        driverConfigBuilder
                .withStringList(DefaultDriverOption.CONTACT_POINTS, getContactPoints(url))
                .withString(DefaultDriverOption.SESSION_NAME, clusterName);

        this.initSocketOptions(driverConfigBuilder);
        this.initPoolingOptions(driverConfigBuilder);
        this.initQueryOptions(driverConfigBuilder);

        driverConfigBuilder.withString(DefaultDriverOption.PROTOCOL_COMPRESSION,
                StringUtils.isEmpty(this.compression) ? "none" : this.compression.toLowerCase());

        if (this.ssl) {
            driverConfigBuilder.withString(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS,
                    "DefaultSslEngineFactory")
                .withBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION, this.sslHostnameValidation);
            if(!this.sslTrustStore.isEmpty()) {
                driverConfigBuilder.withString(DefaultDriverOption.SSL_TRUSTSTORE_PATH, this.sslTrustStore)
                    .withString(DefaultDriverOption.SSL_TRUSTSTORE_PASSWORD, this.sslTrustStorePassword);
            }
            if(!this.sslKeyStore.isEmpty()) {
                driverConfigBuilder.withString(DefaultDriverOption.SSL_KEYSTORE_PATH, this.sslKeyStore)
                    .withString(DefaultDriverOption.SSL_KEYSTORE_PASSWORD, this.sslKeyStorePassword);
            }
            if(!this.sslCipherSuites.isEmpty()) {
                driverConfigBuilder.withStringList(DefaultDriverOption.SSL_CIPHER_SUITES, this.sslCipherSuites);
            }
        }

        if (this.metrics) {
            driverConfigBuilder.withStringList(DefaultDriverOption.METRICS_SESSION_ENABLED,
                    Arrays.asList(DefaultSessionMetric.CONNECTED_NODES.getPath(),
                            DefaultSessionMetric.CQL_REQUESTS.getPath()));
            driverConfigBuilder.withStringList(DefaultDriverOption.METRICS_NODE_ENABLED,
                    Arrays.asList(DefaultNodeMetric.OPEN_CONNECTIONS.getPath(),
                            DefaultNodeMetric.IN_FLIGHT.getPath()));
        }

        if (this.credentials) {
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_CLASS,
                    "PlainTextAuthProvider");
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME,
                    this.username);
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD,
                    this.password);
        }

        driverConfigBuilder.withBoolean(DefaultDriverOption.RECONNECT_ON_INIT,
                    true);
        driverConfigBuilder.withString(DefaultDriverOption.RECONNECTION_POLICY_CLASS,
                "ExponentialReconnectionPolicy");
        driverConfigBuilder.withDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY,
                Duration.ofMillis(this.initRetryInterval));
        driverConfigBuilder.withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY,
                Duration.ofMillis(this.initTimeout));

        this.loader = driverConfigBuilder.build();
    }

    protected ConsistencyLevel getDefaultReadConsistencyLevel() {
        if (defaultReadConsistencyLevel == null) {
            if (readConsistencyLevel != null) {
                defaultReadConsistencyLevel = DefaultConsistencyLevel.valueOf(readConsistencyLevel.toUpperCase());
            } else {
                defaultReadConsistencyLevel = DefaultConsistencyLevel.ONE;
            }
        }
        return defaultReadConsistencyLevel;
    }

    protected ConsistencyLevel getDefaultWriteConsistencyLevel() {
        if (defaultWriteConsistencyLevel == null) {
            if (writeConsistencyLevel != null) {
                defaultWriteConsistencyLevel = DefaultConsistencyLevel.valueOf(writeConsistencyLevel.toUpperCase());
            } else {
                defaultWriteConsistencyLevel = DefaultConsistencyLevel.ONE;
            }
        }
        return defaultWriteConsistencyLevel;
    }

    private void initSocketOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT,
                Duration.ofMillis(this.connectTimeoutMillis));
        driverConfigBuilder.withDuration(DefaultDriverOption.REQUEST_TIMEOUT,
                Duration.ofMillis(this.readTimeoutMillis));
        if (this.keepAlive != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_KEEP_ALIVE,
                    this.keepAlive);
        }
        if (this.reuseAddress != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_REUSE_ADDRESS,
                    this.reuseAddress);
        }
        if (this.soLinger != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_LINGER_INTERVAL,
                    this.soLinger);
        }
        if (this.tcpNoDelay != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_TCP_NODELAY,
                    this.tcpNoDelay);
        }
        if (this.receiveBufferSize != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE,
                    this.receiveBufferSize);
        }
        if (this.sendBufferSize != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE,
                    this.sendBufferSize);
        }
    }

    private void initPoolingOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS,
                this.max_requests_local);
    }

    private void initQueryOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withInt(DefaultDriverOption.REQUEST_PAGE_SIZE,
                this.defaultFetchSize);
    }

    private List<String> getContactPoints(String url) {
        List<String> result;
        if (StringUtils.isBlank(url)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : url.split(COMMA)) {
                result.add(hostPort);
            }
        }
        return result;
    }

}
