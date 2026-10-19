// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.util.TbAutoConfiguration;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sqlts.timescale"})
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sqlts.timescale", "org.thingsboard.server.dao.sqlts.insert.timescale"}, bootstrapMode = BootstrapMode.LAZY)
@EnableTransactionManagement
@TimescaleDBTsDao
public class TimescaleDaoConfig {

}
