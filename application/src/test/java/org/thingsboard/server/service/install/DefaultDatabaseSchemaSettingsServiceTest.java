// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDatabaseSchemaSettingsServiceTest {

    private static final String PACKAGE_VERSION = "4.4.0";
    private static final long V_4_3_1_4 = 4_003_001_004L;

    @Mock
    private ProjectInfo projectInfo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DefaultDatabaseSchemaSettingsService service;

    @Test
    void updateSchemaVersionWithExplicitVersionEncodesAsLong() {
        service.updateSchemaVersion("4.2.2.3");
        verify(jdbcTemplate).execute("UPDATE tb_schema_settings SET schema_version = 4002002003");
    }

    @Test
    void updateSchemaVersionWithShortVersionPadsMissingComponents() {
        service.updateSchemaVersion("4.3");
        verify(jdbcTemplate).execute("UPDATE tb_schema_settings SET schema_version = 4003000000");
    }

    @Test
    void aCeDatabaseAtTheSupportedVersionIsAcceptedAndTakesTheConversionBranch() {
        givenSchemaSettings("CE", V_4_3_1_4);

        service.validateSchemaSettings();

        assertThat(service.isUpgradeFromCe()).isTrue();
    }

    @Test
    void aPeDatabaseAtASupportedVersionIsAcceptedAndTakesThePlainUpgradeBranch() {
        givenSchemaSettings("PE", V_4_3_1_4);

        service.validateSchemaSettings();

        assertThat(service.isUpgradeFromCe()).isFalse();
    }

    @Test
    void aCeDatabaseOnALaterPatchOfTheSupportedLineIsAlsoAccepted() {
        // 4.3 keeps receiving patches, so the CE floor is a minimum rather than a single release.
        givenSchemaSettings("CE", 4_003_001_005L);

        service.validateSchemaSettings();

        assertThat(service.isUpgradeFromCe()).isTrue();
    }

    @Test
    void aCeDatabaseNewerThanThePePackageIsRejected() {
        givenSchemaSettings("CE", 4_004_000_001L);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("newer than this ThingsBoard PE package");
    }

    @Test
    void aCeDatabaseBelowTheSupportedVersionIsRejectedAtTheWall() {
        // 4.3.0.0: within the versions the PE branch accepts, but below the CE release that converts.
        givenSchemaSettings("CE", 4_003_000_000L);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("transitioning from CE to PE")
                .hasMessageContaining("4.3.1.4");
    }

    @Test
    void aPeDatabaseAlreadyAtThePackageVersionIsRejected() {
        givenSchemaSettings("PE", 4_004_000_000L);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("already upgraded to current version");
    }

    @Test
    void aPeDatabaseAtAnUnsupportedVersionIsRejected() {
        givenSchemaSettings("PE", 4_002_002_000L);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("is not supported for upgrade");
    }

    @Test
    void aDatabaseWithAnUnknownProductMarkerIsRejected() {
        // Neither CE nor PE: unchecked, the marker would silently select the PE branch and the CE-to-PE
        // conversion would be skipped on a database that needs it.
        givenSchemaSettings("TB", V_4_3_1_4);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("unrecognized product");
    }

    @Test
    void aDatabaseWithoutAProductMarkerIsRejected() {
        givenSchemaSettings(null, V_4_3_1_4);

        assertThatThrownBy(() -> service.validateSchemaSettings())
                .hasMessageContaining("unrecognized product");
    }

    @Test
    void anUnrecognizedProductMarkerIsRejectedByTheBranchSelectorItself() {
        // The guard has to live where the branch is chosen: validateSchemaSettings() returns early when
        // SKIP_SCHEMA_VERSION_CHECK is set, and the upgrade then asks isUpgradeFromCe() regardless.
        givenSchemaSettings("TB", V_4_3_1_4);

        assertThatThrownBy(() -> service.isUpgradeFromCe())
                .hasMessageContaining("unrecognized product");
    }

    @Test
    void aMissingProductMarkerIsRejectedByTheBranchSelectorItself() {
        givenSchemaSettings(null, V_4_3_1_4);

        assertThatThrownBy(() -> service.isUpgradeFromCe())
                .hasMessageContaining("unrecognized product");
    }

    private void givenSchemaSettings(String product, long schemaVersion) {
        when(projectInfo.getProjectVersion()).thenReturn(PACKAGE_VERSION);
        when(projectInfo.getProductType()).thenReturn("PE");
        when(jdbcTemplate.queryForList("SELECT product FROM tb_schema_settings", String.class))
                .thenReturn(product == null ? List.of() : List.of(product));
        when(jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class))
                .thenReturn(List.of(schemaVersion));
    }

}
