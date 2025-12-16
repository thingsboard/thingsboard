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
package org.thingsboard.server.dao;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.CassandraContainer;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public abstract class AbstractNoSqlContainer {

    public static final List<String> INIT_SCRIPTS = List.of(
            "cassandra/schema-keyspace.cql",
            "cassandra/schema-ts.cql",
            "cassandra/schema-ts-latest.cql"
    );

    public static final CassandraContainer<?> cassandraContainer = new CassandraContainer<>("cassandra:5.0")
            .withEnv("HEAP_NEWSIZE", "64M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withEnv("CASSANDRA_CLUSTER_NAME", "ThingsBoard Cluster");

    @ClassRule(order = 1)
    public static final ExternalResource cassandra = new ExternalResource() {
        private CqlSession session;

        @Override
        protected void before() throws Throwable {
            cassandraContainer.start();

            session = CqlSession.builder()
                    .addContactPoint(cassandraContainer.getContactPoint())
                    .withLocalDatacenter("datacenter1")
                    .build();

            for (String script : INIT_SCRIPTS) {
                runInitScriptIfRequired(script);
            }

            String cassandraUrl = String.format("%s:%s", cassandraContainer.getHost(), cassandraContainer.getMappedPort(9042));
            log.debug("Cassandra url [{}]", cassandraUrl);
            System.setProperty("cassandra.url", cassandraUrl);
        }

        @Override
        protected void after() {
            if (session != null) {
                session.close();
            }
            cassandraContainer.stop();
            List.of("cassandra.url").forEach(System.getProperties()::remove);
        }

        private void runInitScriptIfRequired(String initScriptPath) {
            log.info("Init script [{}]", initScriptPath);
            if (StringUtils.isNotBlank(initScriptPath)) {
                try {
                    URL resource = Thread.currentThread().getContextClassLoader().getResource(initScriptPath);
                    if (resource == null) {
                        log.warn("Could not load classpath init script: {}", initScriptPath);
                        throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath + ". Resource not found.");
                    }
                    String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                    for (String stmt : cql.split(";")) {
                        if (StringUtils.isNotBlank(stmt)) {
                            session.execute(stmt);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Could not load classpath init script: {}", initScriptPath);
                    throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
                } catch (Exception e) {
                    log.error("Error while executing init script: {}", initScriptPath, e);
                    throw new UncategorizedScriptException("Error while executing init script: " + initScriptPath, e);
                }
            }
        }
    };

    public static class ScriptLoadException extends RuntimeException {
        public ScriptLoadException(String message) { super(message); }
        public ScriptLoadException(String message, Throwable cause) { super(message, cause); }
    }

    public static class UncategorizedScriptException extends RuntimeException {
        public UncategorizedScriptException(String message, Throwable cause) { super(message, cause); }
    }
}
