// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.util.SqlTsLatestDao;
import org.thingsboard.server.dao.util.TbAutoConfiguration;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sqlts.sql"})
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sqlts.insert.latest.sql", "org.thingsboard.server.dao.sqlts.latest"}, bootstrapMode = BootstrapMode.LAZY)
@EnableTransactionManagement
@SqlTsLatestDao
public class SqlTsLatestDaoConfig {

}
