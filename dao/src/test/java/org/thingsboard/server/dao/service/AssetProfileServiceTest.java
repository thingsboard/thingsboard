/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class AssetProfileServiceTest extends AbstractServiceTest {

    private IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    @Autowired
    AssetProfileService assetProfileService;
    @Autowired
    AssetService assetService;

    @Test
    public void testSaveAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());
        savedAssetProfile.setName("New asset profile");
        assetProfileService.saveAssetProfile(savedAssetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());
    }

    @Test
    public void testFindAssetProfileById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    public void testFindAssetProfileInfoById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfileInfo foundAssetProfileInfo = assetProfileService.findAssetProfileInfoById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void testFindDefaultAssetProfile() {
        AssetProfile foundDefaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertNotNull(foundDefaultAssetProfile.getId());
        Assert.assertNotNull(foundDefaultAssetProfile.getName());
    }

    @Test
    public void testFindDefaultAssetProfileInfo() {
        AssetProfileInfo foundDefaultAssetProfileInfo = assetProfileService.findDefaultAssetProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
    }

    @Test
    public void testFindOrCreateAssetProfile() throws ExecutionException, InterruptedException {
        ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            List<ListenableFuture<AssetProfile>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 1")));
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 2")));
            }

            List<AssetProfile> assetProfiles = Futures.allAsList(futures).get();
            assetProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void testSetDefaultAssetProfile() {
        AssetProfile assetProfile1 = this.createAssetProfile(tenantId, "Asset Profile 1");
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile 2");

        AssetProfile savedAssetProfile1 = assetProfileService.saveAssetProfile(assetProfile1);
        AssetProfile savedAssetProfile2 = assetProfileService.saveAssetProfile(assetProfile2);

        boolean result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile1.getId());
        Assert.assertTrue(result);
        AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile1.getId(), defaultAssetProfile.getId());
        result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile2.getId());
        Assert.assertTrue(result);
        defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile2.getId(), defaultAssetProfile.getId());
    }

    @Test
    public void testSaveAssetProfileWithEmptyName() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile);
        });
    }

    @Test
    public void testSaveAssetProfileWithSameName() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile2);
        });
    }

    @Test
    public void testSaveAssetProfileWithNameDefaultAndProfileWithIsDefaultAndDifferentNameAlreadyExists() {
        AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        defaultAssetProfile.setName("Asset Profile 1");
        assetProfileService.saveAssetProfile(defaultAssetProfile);
        AssetProfile createdAssetProfile = assetProfileService.findOrCreateAssetProfile(tenantId, "default");
        Assert.assertNotNull(createdAssetProfile);
        Assert.assertEquals("default", createdAssetProfile.getName());
        Assert.assertFalse(createdAssetProfile.isDefault());
    }

    @Test
    public void testDeleteAssetProfileWithExistingAsset() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());
        assetService.saveAsset(asset);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        });
    }

    @Test
    public void testDeleteAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNull(foundAssetProfile);
    }

    @Test
    public void testFindAssetProfiles() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        for (AssetProfile assetProfile : loadedAssetProfiles) {
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAssetProfileInfos() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream()
                .map(assetProfile -> new AssetProfileInfo(assetProfile.getId(), assetProfile.getTenantId(),
                        assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAllassetProfilesByTenantId() {
        int assetProfilesCount = 4; // 3 created + default
        var assetProfiles = new ArrayList<AssetProfile>(4);

        var profileC = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile C"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileC));


        var profileA = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile A"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileA));


        var profileB = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile B"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileB));


        assetProfiles.add(assetProfileService.findDefaultAssetProfile(tenantId));

        List<EntityInfo> sortedProfileInfos = assetProfiles.stream()
                .map(profile -> new EntityInfo(profile.getId(), profile.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());

        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, false);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).hasSize(assetProfilesCount);
        assertThat(assetProfileInfos).isEqualTo(sortedProfileInfos);
    }

    @Test
    public void testFindActiveOnlyassetProfilesByTenantId() {

        String profileCName = "profile C";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileCName));

        String profileAName = "profile A";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileAName));

        String profileBName = "profile B";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileBName));


        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).isEmpty();

        var assetC = new Asset();
        assetC.setName("Test asset C");
        assetC.setType(profileCName);
        assetC.setTenantId(tenantId);

        assetC = assetService.saveAsset(assetC);

        var assetA = new Asset();
        assetA.setName("Test asset A");
        assetA.setType(profileAName);
        assetA.setTenantId(tenantId);

        assetA = assetService.saveAsset(assetA);

        var assetB = new Asset();
        assetB.setName("Test asset B");
        assetB.setType(profileBName);
        assetB.setTenantId(tenantId);

        assetB = assetService.saveAsset(assetB);

        assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        var expected = List.of(
                new EntityInfo(assetA.getAssetProfileId(), profileAName),
                new EntityInfo(assetB.getAssetProfileId(), profileBName),
                new EntityInfo(assetC.getAssetProfileId(), profileCName)
        );

        assertThat(assetProfileInfos).isNotEmpty();
        assertThat(assetProfileInfos).hasSize(3);
        assertThat(assetProfileInfos).isEqualTo(expected);
    }


}
