// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultEntityQueryRepository.class)
public class DefaultEntityQueryRepositoryTest {

    @MockitoBean
    NamedParameterJdbcTemplate jdbcTemplate;
    @MockitoBean
    TransactionTemplate transactionTemplate;
    @MockitoBean
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
