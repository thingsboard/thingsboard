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
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultEntityQueryRepository.class)
public class DefaultEntityQueryRepositoryTest {

    @MockBean
    NamedParameterJdbcTemplate jdbcTemplate;
    @MockBean
    TransactionTemplate transactionTemplate;
    @MockBean
    DefaultQueryLogComponent queryLog;

    @Autowired
    DefaultEntityQueryRepository repo;

    /*
     * This value has to be reasonable small to prevent infinite recursion as early as possible
     * */
    @Test
    public void givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo() {
        assertThat(repo.getMaxLevelAllowed(), equalTo(50));
    }

    @Test
    public void givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel() {
        assertThat(repo.getMaxLevel(0), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-2), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MIN_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

    @Test
    public void givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame() {
        assertThat(repo.getMaxLevel(1), equalTo(1));
        assertThat(repo.getMaxLevel(2), equalTo(2));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed()), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed() + 1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MAX_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

}
