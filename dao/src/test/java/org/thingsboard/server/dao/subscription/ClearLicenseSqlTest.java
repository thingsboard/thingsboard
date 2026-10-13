// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * What {@code POST /api/admin/license/clear} durably does, against the real {@code tb_cluster} row. The
 * controller test cannot reach this: the test profile stands
 * {@code InstallSubscriptionService} in for the licensed services, and its {@code clearLicense}
 * is a no-op, so the endpoint's HTTP contract and the state it clears have to be pinned separately.
 */
@DaoSqlTest
public class ClearLicenseSqlTest extends AbstractServiceTest {

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void tearDown() {
        // tb_cluster is the single shared row: leave the columns as the installer does, whatever the test did.
        jdbcTemplate.update("UPDATE tb_cluster SET license_secret = NULL, license_claim_token = NULL");
    }

    @Test
    public void clearingTheLicenseEmptiesTheStoredSecretAndClaimToken() throws IOException {
        tbClusterStore.saveLicenseSecret("a-license-secret");
        tbClusterStore.saveLicenseClaimToken("a-claim-token");
        Path instanceDataFile = Files.createTempFile("instance-license", ".data");

        BasicLicenseActivationService licenseActivationService = activatedService(instanceDataFile);
        licenseActivationService.clearLicense();

        // The secret is what a whole cluster shares its activation through, so an instance still holding it
        // would re-activate itself on the next request instead of returning to the wizard.
        assertThat(tbClusterStore.getLicenseSecret()).isEmpty();
        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
        assertThat(instanceDataFile).doesNotExist();
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
    }

    @Test
    public void clearingTheLicenseDropsAClaimTokenNobodyIsWaitingOn() throws IOException {
        // Unconditionally, unlike the compare-and-clear a finished claim uses: an outstanding claim for the
        // licence being removed must not survive into the next activation.
        tbClusterStore.saveLicenseClaimToken("a-claim-token");

        activatedService(Files.createTempFile("instance-license", ".data")).clearLicense();

        assertThat(tbClusterStore.getLicenseClaimToken()).isEmpty();
    }

    private BasicLicenseActivationService activatedService(Path instanceDataFile) {
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "instanceDataFilePath", instanceDataFile.toString());
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "licenseVersion", 2);
        return licenseActivationService;
    }

}
