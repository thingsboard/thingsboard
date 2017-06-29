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

package org.thingsboard.server.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.CassandraInstallCluster;
import org.thingsboard.server.install.cql.CQLStatementsParser;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.SystemDataLoaderService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    private static final String SCHEMA_CQL = "schema.cql";

    @Value("${install.data_dir}")
    private String dataDir;

    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

    @Autowired
    private CassandraInstallCluster cluster;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    public void performInstall() {
        try {
            log.info("Starting ThingsBoard Installation...");

            if (this.dataDir == null) {
                throw new RuntimeException("'install.data_dir' property should specified!");
            }
            if (!Files.isDirectory(Paths.get(this.dataDir))) {
                throw new RuntimeException("'install.data_dir' property value is not a valid directory!");
            }

            log.info("Installing DataBase schema...");

            Path schemaFile = Paths.get(this.dataDir, SCHEMA_CQL);
            loadCql(schemaFile);

            log.info("Loading system data...");

            componentDiscoveryService.discoverComponents();

            systemDataLoaderService.createSysAdmin();
            systemDataLoaderService.createAdminSettings();
            systemDataLoaderService.loadSystemWidgets();
            systemDataLoaderService.loadSystemPlugins();
            systemDataLoaderService.loadSystemRules();

            if (loadDemo) {
                log.info("Loading demo data...");
                systemDataLoaderService.loadDemoData();
            }

            log.info("Finished!");
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> cluster.getSession().execute(statement));
    }

}