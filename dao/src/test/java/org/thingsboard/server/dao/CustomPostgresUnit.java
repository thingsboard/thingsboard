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
package org.thingsboard.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V9_6;

/**
 * Created by Valerii Sosliuk on 6/24/2017.
 */
@Slf4j
public class CustomPostgresUnit extends ExternalResource {

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String DATABASE = "database";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private List<String> sqlFiles;
    private Properties properties;

    private EmbeddedPostgres postgres;

    public CustomPostgresUnit(List<String> sqlFiles, String configurationFileName) {
        this.sqlFiles = sqlFiles;
        this.properties = loadProperties(configurationFileName);
    }

    @Override
    public void before() {
        postgres = new EmbeddedPostgres(V9_6);
        load();
    }

    @Override
    public void after() {
        postgres.stop();
    }

    private void load() {
        Connection conn = null;
        try {
            String url = postgres.start(properties.getProperty(HOST),
                                        Integer.parseInt(properties.getProperty(PORT)),
                                        properties.getProperty(DATABASE),
                                        properties.getProperty(USERNAME),
                                        properties.getProperty(PASSWORD));

            conn = DriverManager.getConnection(url);
            for (String sqlFile : sqlFiles) {
                URL sqlFileUrl = Resources.getResource(sqlFile);
                String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to start embedded postgres. Reason: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private Properties loadProperties(String fileName) {
        final Properties properties = new Properties();
        try (final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
