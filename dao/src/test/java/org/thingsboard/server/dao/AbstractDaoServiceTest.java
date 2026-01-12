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
package org.thingsboard.server.dao;

import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean(answer = Answers.RETURNS_MOCKS)
    StatsFactory statsFactory;

}
