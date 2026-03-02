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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DaoSqlTest
public class TenantProfileServiceTest extends AbstractServiceTest {

    @Autowired
    TenantProfileService tenantProfileService;

    private IdComparator<TenantProfile> idComparator = new IdComparator<>();
    private IdComparator<EntityInfo> tenantProfileInfoIdComparator = new IdComparator<>();

    @Before
    public void before() {
        //this test requires no Tenants in the database
        tenantId = null;
        tenantService.deleteTenants();
        tenantProfileService.deleteTenantProfiles(TenantId.SYS_TENANT_ID);
    }

    @After
    public void after() {
        tenantProfileService.deleteTenantProfiles(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveTenantProfile() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");

        tenantProfile.setIsolatedTbRuleEngine(true);

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        mainQueueConfiguration.setAdditionalInfo(NullNode.getInstance());
        tenantProfile.getProfileData().setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Assert.assertNotNull(savedTenantProfile);
        Assert.assertNotNull(savedTenantProfile.getId());
        Assert.assertTrue(savedTenantProfile.getCreatedTime() > 0);
        Assert.assertEquals(tenantProfile.getName(), savedTenantProfile.getName());
        Assert.assertEquals(tenantProfile.getDescription(), savedTenantProfile.getDescription());
        Assert.assertEquals(tenantProfile.getProfileData(), savedTenantProfile.getProfileData());
        Assert.assertEquals(tenantProfile.isDefault(), savedTenantProfile.isDefault());
        Assert.assertEquals(tenantProfile.isIsolatedTbRuleEngine(), savedTenantProfile.isIsolatedTbRuleEngine());

        savedTenantProfile.setName("New tenant profile");
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile);
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertEquals(foundTenantProfile.getName(), savedTenantProfile.getName());
    }

    @Test
    public void testFindTenantProfileById() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNotNull(foundTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundTenantProfile);
    }

    @Test
    public void testFindTenantProfileInfoById() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        EntityInfo foundTenantProfileInfo = tenantProfileService.findTenantProfileInfoById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNotNull(foundTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundTenantProfileInfo.getName());
    }

    @Test
    public void testFindDefaultTenantProfile() {
        TenantProfile tenantProfile = this.createTenantProfile("Default Tenant Profile");
        tenantProfile.setDefault(true);
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        TenantProfile foundDefaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundDefaultTenantProfile);
    }

    @Test
    public void testFindDefaultTenantProfileInfo() {
        TenantProfile tenantProfile = this.createTenantProfile("Default Tenant Profile");
        tenantProfile.setDefault(true);
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        EntityInfo foundDefaultTenantProfileInfo = tenantProfileService.findDefaultTenantProfileInfo(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(foundDefaultTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundDefaultTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundDefaultTenantProfileInfo.getName());
    }

    @Test
    public void testSetDefaultTenantProfile() {
        TenantProfile tenantProfile1 = this.createTenantProfile("Tenant Profile 1");
        TenantProfile tenantProfile2 = this.createTenantProfile("Tenant Profile 2");

        TenantProfile savedTenantProfile1 = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile1);
        TenantProfile savedTenantProfile2 = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile2);

        TenantProfile setDefaultTenantProfile1 = tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile1.getId());
        Assert.assertNotEquals(savedTenantProfile1.isDefault(), setDefaultTenantProfile1.isDefault());
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(defaultTenantProfile);
        Assert.assertEquals(savedTenantProfile1.getId(), defaultTenantProfile.getId());
        TenantProfile setDefaultTenantProfile2 = tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile2.getId());
        Assert.assertNotEquals(savedTenantProfile2.isDefault(), setDefaultTenantProfile2.isDefault());
        defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(defaultTenantProfile);
        Assert.assertEquals(savedTenantProfile2.getId(), defaultTenantProfile.getId());
    }

    @Test
    public void testSaveTenantProfileWithEmptyName() {
        TenantProfile tenantProfile = new TenantProfile();
        Assertions.assertThrows(DataValidationException.class, () -> {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        });
    }

    @Test
    public void testSaveTenantProfileWithSameName() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        TenantProfile tenantProfile2 = this.createTenantProfile("Tenant Profile");
        Assertions.assertThrows(DataValidationException.class, () -> {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile2);
        });
    }

    @Test
    public void testDeleteTenantProfileWithExistingTenant() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        tenant = tenantService.saveTenant(tenant);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
            });
        } finally {
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testDeleteTenantProfile() {
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNull(foundTenantProfile);
    }

    @Test
    public void testFindTenantProfiles() {

        List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenantProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile));
        }

        List<TenantProfile> loadedTenantProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
            loadedTenantProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfiles, idComparator);

        Assert.assertEquals(tenantProfiles, loadedTenantProfiles);

        for (TenantProfile tenantProfile : loadedTenantProfiles) {
            tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile.getId());
        }

        pageLink = new PageLink(17);
        pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindTenantProfileInfos() {

        List<TenantProfile> tenantProfiles = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile));
        }

        List<EntityInfo> loadedTenantProfileInfos = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = tenantProfileService.findTenantProfileInfos(TenantId.SYS_TENANT_ID, pageLink);
            loadedTenantProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfileInfos, tenantProfileInfoIdComparator);

        List<EntityInfo> tenantProfileInfos = tenantProfiles.stream().map(tenantProfile -> new EntityInfo(tenantProfile.getId(),
                tenantProfile.getName())).collect(Collectors.toList());

        Assert.assertEquals(tenantProfileInfos, loadedTenantProfileInfos);

        for (EntityInfo tenantProfile : loadedTenantProfileInfos) {
            tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, new TenantProfileId(tenantProfile.getId().getId()));
        }

        pageLink = new PageLink(17);
        pageData = tenantProfileService.findTenantProfileInfos(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testTenantProfileSerialization_proto() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setId(new TenantProfileId(UUID.randomUUID()));
        tenantProfile.setName("testProfile");
        TenantProfileData profileData = new TenantProfileData();
        tenantProfile.setProfileData(profileData);
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        addMainQueueConfig(tenantProfile);

        byte[] serialized = assertDoesNotThrow(() -> ProtoUtils.toProto(tenantProfile).toByteArray());

        TenantProfile deserialized = assertDoesNotThrow(() -> ProtoUtils.fromProto(TransportProtos.TenantProfileProto.parseFrom(serialized)));
        assertThat(deserialized).isEqualTo(tenantProfile);
        assertThat(deserialized.getProfileData()).isNotNull();
        assertThat(deserialized.getProfileData().getQueueConfiguration()).isNotEmpty();
    }

    public static TenantProfile createTenantProfile(String name) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(profileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);
        return tenantProfile;
    }

    public static void addMainQueueConfig(TenantProfile tenantProfile) {
        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        TenantProfileData profileData = tenantProfile.getProfileData();
        profileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));
        tenantProfile.setProfileData(profileData);
    }
}
