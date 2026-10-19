// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig;
import org.thingsboard.server.dao.config.JpaDaoConfig;
import org.thingsboard.server.dao.config.SqlTsDaoConfig;
import org.thingsboard.server.dao.config.SqlTsLatestDaoConfig;
import org.thingsboard.server.dao.service.DaoSqlTest;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {JpaDaoConfig.class, SqlTsDaoConfig.class, SqlTsLatestDaoConfig.class, DedicatedEventsJpaDaoConfig.class})
@DaoSqlTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class})
public abstract class AbstractDaoServiceTest {

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    StatsFactory statsFactory;

}
