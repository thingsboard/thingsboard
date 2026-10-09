// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DaoSqlTest
public class TbClusterStoreTest extends AbstractServiceTest {

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID clusterId = UUID.randomUUID();

    @Before
    public void seedClusterRow() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?)", clusterId);
    }

    @After
    public void cleanUpClusterRow() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
    }

    @Test
    public void testGetClusterId() {
        assertThat(tbClusterStore.getClusterId()).contains(clusterId);
    }

    @Test
    public void testGetClusterIdOnEmptyTable() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        assertThat(tbClusterStore.getClusterId()).isEmpty();
    }

    @Test
    public void testSecondRowInsertIsRejectedByUniqueIndex() {
        UUID other = new UUID(0, 0);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?)", other))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(tbClusterStore.getClusterId()).contains(clusterId);
    }

    @Test
    public void testSaveAndReadLicenseClaimToken() {
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
        tbClusterStore.saveLicenseClaimToken("token-one");
        assertThat(tbClusterStore.getLicenseClaimToken()).contains("token-one");
        tbClusterStore.saveLicenseClaimToken("token-two");
        assertThat(tbClusterStore.getLicenseClaimToken()).contains("token-two");
    }

    @Test
    public void testSaveAndReadLicenseSecret() {
        assertThat(tbClusterStore.getLicenseSecret()).isEmpty();
        tbClusterStore.saveLicenseSecret("secret-one");
        assertThat(tbClusterStore.getLicenseSecret()).contains("secret-one");
        tbClusterStore.saveLicenseSecret("secret-two");
        assertThat(tbClusterStore.getLicenseSecret()).contains("secret-two");
    }

    @Test
    public void testLicenseSecretAndClaimTokenAreStoredApart() {
        tbClusterStore.saveLicenseSecret("secret-one");
        tbClusterStore.saveLicenseClaimToken("token-one");
        tbClusterStore.clearLicenseClaimToken("token-one");

        assertThat(tbClusterStore.getLicenseSecret()).contains("secret-one");
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    @Test
    public void testSaveLicenseSecretFailsWhenClusterRowIsMissing() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        assertThatThrownBy(() -> tbClusterStore.saveLicenseSecret("secret-one"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(tbClusterStore.getLicenseSecret()).isEqualTo(Optional.empty());
    }

    @Test
    public void testClearLicenseClaimToken() {
        tbClusterStore.saveLicenseClaimToken("token-one");
        tbClusterStore.forceClearLicenseClaimToken();
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    @Test
    public void testCompareAndClearRetiresOnlyTheExpectedToken() {
        tbClusterStore.saveLicenseClaimToken("token-one");
        tbClusterStore.clearLicenseClaimToken("token-one");
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();

        tbClusterStore.saveLicenseClaimToken("token-two");
        tbClusterStore.clearLicenseClaimToken("token-one");
        assertThat(tbClusterStore.getLicenseClaimToken()).contains("token-two");
    }

    @Test
    public void testSaveFailsWhenClusterRowIsMissing() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        assertThatThrownBy(() -> tbClusterStore.saveLicenseClaimToken("token-one"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(tbClusterStore.getLicenseClaimToken()).isEqualTo(Optional.empty());
    }

    @Test
    public void testClearNeverThrowsWhenClusterRowIsMissing() {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        tbClusterStore.forceClearLicenseClaimToken();
        tbClusterStore.clearLicenseClaimToken("token-one");
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    @Test
    public void testClearSwallowsAStoreFailureRatherThanPropagatingIt() {
        JdbcTemplate failing = mock(JdbcTemplate.class, invocation -> {
            throw new DataAccessResourceFailureException("the database is unreachable");
        });
        TbClusterStore store = new TbClusterStore(failing);

        assertThatCode(store::forceClearLicenseClaimToken).doesNotThrowAnyException();
        assertThatCode(() -> store.clearLicenseClaimToken("token-one")).doesNotThrowAnyException();
    }
}
