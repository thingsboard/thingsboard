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

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import javax.script.ScriptException;
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

    @ClassRule(order = 0)
    public static final CassandraContainer cassandra = (CassandraContainer) new CassandraContainer("cassandra:5.0") {
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            DatabaseDelegate db = new CassandraDatabaseDelegate(this);
            INIT_SCRIPTS.forEach(script -> runInitScriptIfRequired(db, script));
        }

        private void runInitScriptIfRequired(DatabaseDelegate db, String initScriptPath) {
            logger().info("Init script [{}]", initScriptPath);
            if (initScriptPath != null) {
                try {
                    URL resource = Thread.currentThread().getContextClassLoader().getResource(initScriptPath);
                    if (resource == null) {
                        logger().warn("Could not load classpath init script: {}", initScriptPath);
                        throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath + ". Resource not found.");
                    }
                    String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                    ScriptUtils.executeDatabaseScript(db, initScriptPath, cql);
                } catch (IOException e) {
                    logger().warn("Could not load classpath init script: {}", initScriptPath);
                    throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
                } catch (ScriptException e) {
                    logger().error("Error while executing init script: {}", initScriptPath, e);
                    throw new ScriptUtils.UncategorizedScriptException("Error while executing init script: " + initScriptPath, e);
                }
            }
        }
    }
            .withEnv("HEAP_NEWSIZE", "64M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withEnv("CASSANDRA_CLUSTER_NAME", "ThingsBoard Cluster");

    @ClassRule(order = 1)
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            cassandra.start();
            String cassandraUrl = String.format("%s:%s", cassandra.getHost(), cassandra.getMappedPort(9042));
            log.debug("Cassandra url [{}]", cassandraUrl);
            System.setProperty("cassandra.url", cassandraUrl);
        }

        @Override
        protected void after() {
            cassandra.stop();
            List.of("cassandra.url")
                    .forEach(System.getProperties()::remove);
        }
    };

}
