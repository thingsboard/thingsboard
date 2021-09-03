package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.thingsboard.server.common.data.id.CustomerId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

public class DefaultEntityQueryRepositoryTest {

    /*
     * This value has to be reasonable small to prevent infinite recursion as early as possible
     * */
    @Test
    public void givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo() {
        assertThat(DefaultEntityQueryRepository.MAX_LEVEL_DEFAULT, equalTo(10));
    }

    @Test
    public void givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel() {
        DefaultEntityQueryRepository repo = mock(DefaultEntityQueryRepository.class);
        willCallRealMethod().given(repo).getMaxLevel(anyInt());
        assertThat(repo.getMaxLevel(0), equalTo(DefaultEntityQueryRepository.MAX_LEVEL_DEFAULT));
        assertThat(repo.getMaxLevel(-1), equalTo(DefaultEntityQueryRepository.MAX_LEVEL_DEFAULT));
        assertThat(repo.getMaxLevel(-2), equalTo(DefaultEntityQueryRepository.MAX_LEVEL_DEFAULT));
        assertThat(repo.getMaxLevel(Integer.MIN_VALUE), equalTo(DefaultEntityQueryRepository.MAX_LEVEL_DEFAULT));
    }

    @Test
    public void givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame() {
        DefaultEntityQueryRepository repo = mock(DefaultEntityQueryRepository.class);
        willCallRealMethod().given(repo).getMaxLevel(anyInt());
        assertThat(repo.getMaxLevel(1), equalTo(1));
        assertThat(repo.getMaxLevel(2), equalTo(2));
        assertThat(repo.getMaxLevel(Integer.MAX_VALUE), equalTo(Integer.MAX_VALUE));
    }

}
