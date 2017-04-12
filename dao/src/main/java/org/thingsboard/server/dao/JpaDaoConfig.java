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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Valerii Sosliuk
 */
@Configuration
@ConditionalOnProperty(prefix="sql", value="enabled",havingValue = "true", matchIfMissing = false)
public class JpaDaoConfig {

    @Value("sql.datasource.url")
    private String url;
    @Value("sql.datasource.username")
    private String username;
    @Value("sql.datasource.password")
    private String password;

    public DataSource dataSource() {
        return DataSourceBuilder.create().url(url).username(username).password(password).build();
    }
}
