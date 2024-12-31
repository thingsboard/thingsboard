/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.util.TDengineTsDao;
import org.thingsboard.server.dao.util.TDengineTsOrTsLatestDao;

import javax.sql.DataSource;

/**
 * for tdengine
 */
@Slf4j
@Configuration
@TDengineTsOrTsLatestDao
public class DataSourceConfig {

    @Value("${spring.datasource.driverClassName}")
    private String driverName;
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${tdengine.driverClassName}")
    private String tdDriverName;
    @Value("${tdengine.url}")
    private String tdUrl;
    @Value("${tdengine.username}")
    private String tdUsername;
    @Value("${tdengine.password}")
    private String tdPassword;

    // @Primary
    @Bean
    public DataSource getDatasource() {
        return DataSourceBuilder.create().driverClassName(driverName).url(url).username(username).password(password).build();
    }

    @Qualifier("TDengineDataSource")
    @Bean(name = "TDengineDataSource")
    @TDengineTsDao
    public DataSource tDengineDataSource() {
        HikariConfig config = new HikariConfig();
        // jdbc properties
        config.setJdbcUrl(tdUrl);
        config.setDriverClassName(tdDriverName);
        config.setUsername(tdUsername);
        config.setPassword(tdPassword);
        // connection pool configurations
        config.setMinimumIdle(50);           //minimum number of idle connection
        config.setMaximumPoolSize(100);      //maximum number of connection in the pool
        // config.setConnectionTimeout(30000); //maximum wait milliseconds for get connection from pool
        // config.setMaxLifetime(0);       // maximum life time for each connection
        // config.setIdleTimeout(0);       // max idle time for recycle idle connection

        HikariDataSource ds = new HikariDataSource(config); //create datasource
        return ds;
    }

    @Autowired
    @Bean
    // @Primary
    public JdbcTemplate getTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Autowired
    @Bean(name = "TDengineTemplate")
    @Qualifier("TDengineTemplate")
    @TDengineTsDao
    public JdbcTemplate getTDengineTemplate(@Qualifier("TDengineDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}