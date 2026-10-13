// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;

import static org.assertj.core.api.Assertions.assertThatCode;

@CitusDaoSqlTest
public class CitusAssetServiceTest extends AssetServiceTest {

    /**
     * Citus cross-node FK-visibility regression guard for implicit asset profiles - the asset analog of
     * {@link CitusDeviceServiceTest#testSaveDeviceWithBrandNewProfileTypeCommitsProfileBeforeFkCheck}.
     * <p>
     * In production the asset save is wrapped by {@code DefaultTbAssetService.save}'s
     * {@code @Transactional}; the DAO-level {@code AssetService.saveAsset} is not itself transactional,
     * so here we reproduce that caller transaction explicitly with a TransactionTemplate. Inside it the
     * save calls {@code AssetProfileService.findOrCreateAssetProfile}, which freshly inserts the
     * asset_profile (a Citus reference table) and then flushes the asset (distributed table) with an
     * asset_profile_id FK to it — all in one transaction.
     * <p>
     * Citus handles this natively: when a distributed-table write (the asset) depends on a
     * reference-table row written earlier in the same transaction (the fresh asset_profile), the
     * conflicting-parallel-relation-access guard forces the transaction onto a single, sequential
     * connection, so the parent INSERT is visible to the child FK check. No early commit / REQUIRES_NEW
     * is needed (and the {@code citus.all_modifications_commutative=on} flag is only a lock-mode
     * downgrade, not what makes this work). This test pins that guarantee so a future topology or flag
     * change that breaks it fails loudly here.
     */
    @Test
    public void testSaveAssetWithBrandNewProfileTypeCommitsProfileBeforeFkCheck() {
        String brandNewType = "citus-fresh-asset-profile-" + StringUtils.randomAlphabetic(10);
        Assert.assertNull("Precondition: the profile type must not yet exist",
                assetProfileService.findAssetProfileByName(tenantId, brandNewType, false));

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Asset with brand-new profile " + StringUtils.randomAlphabetic(10));
        asset.setType(brandNewType);

        TransactionTemplate callerTransaction = new TransactionTemplate(platformTransactionManager);

        Asset[] saved = new Asset[1];
        assertThatCode(() -> saved[0] = callerTransaction.execute(status -> assetService.saveAsset(asset)))
                .as("Saving an asset with a brand-new asset profile type must not raise a cross-node FK violation under Citus")
                .doesNotThrowAnyException();

        Assert.assertNotNull(saved[0]);
        Assert.assertNotNull("The implicit asset profile must be committed and FK-visible", saved[0].getAssetProfileId());

        AssetProfile createdProfile = assetProfileService.findAssetProfileByName(tenantId, brandNewType, false);
        Assert.assertNotNull("The fresh asset profile must be persisted", createdProfile);
        Assert.assertEquals(createdProfile.getId().getId(), saved[0].getAssetProfileId().getId());

        assetService.deleteAsset(tenantId, saved[0].getId());
    }
}
