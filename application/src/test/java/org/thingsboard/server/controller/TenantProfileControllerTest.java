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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.data.validation.RateLimit;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.TbQueueCallback;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TenantProfileControllerTest extends AbstractControllerTest {

    private IdComparator<TenantProfile> idComparator = new IdComparator<>();
    private IdComparator<EntityInfo> tenantProfileInfoIdComparator = new IdComparator<>();

    @Test
    public void testSaveTenantProfile() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
        Assert.assertNotNull(savedTenantProfile.getId());
        Assert.assertTrue(savedTenantProfile.getCreatedTime() > 0);
        Assert.assertEquals(tenantProfile.getName(), savedTenantProfile.getName());
        Assert.assertEquals(tenantProfile.getDescription(), savedTenantProfile.getDescription());
        Assert.assertEquals(tenantProfile.getProfileData(), savedTenantProfile.getProfileData());
        Assert.assertEquals(tenantProfile.isDefault(), savedTenantProfile.isDefault());
        Assert.assertEquals(tenantProfile.isIsolatedTbRuleEngine(), savedTenantProfile.isIsolatedTbRuleEngine());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.CREATED, 1);

        awaitAuditLog("Wait for async audit log to be persisted (ADDED expected)", savedTenantProfile.getId(), ActionType.ADDED);

        savedTenantProfile.setName("New tenant profile");
        doPost("/api/tenantProfile", savedTenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertEquals(foundTenantProfile.getName(), savedTenantProfile.getName());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.UPDATED, 1);

        awaitAuditLog("Wait for async audit log to be persisted (UPDATED expected)", savedTenantProfile.getId(), ActionType.UPDATED);

    }

    @Test
    public void testSaveTenantProfileWithViolationOfLengthValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = this.createTenantProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/tenantProfile", tenantProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgErrorFieldLength("name"))));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testFindTenantProfileById() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertNotNull(foundTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundTenantProfile);
    }

    @Test
    public void testFindTenantProfileInfoById() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        EntityInfo foundTenantProfileInfo = doGet("/api/tenantProfileInfo/" + savedTenantProfile.getId().getId().toString(), EntityInfo.class);
        Assert.assertNotNull(foundTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundTenantProfileInfo.getName());
    }

    @Test
    public void testFindTenantProfilesByIds() throws Exception {
        loginSysAdmin();
        List<TenantProfile> tenantProfiles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile " + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        List<TenantProfile> expected = tenantProfiles.subList(2, 7);

        String idsParam = expected.stream()
                .map(tp -> tp.getId().getId().toString())
                .collect(Collectors.joining(","));

        TenantProfile[] result = doGet(
                "/api/tenantProfiles?ids=" + idsParam,
                TenantProfile[].class
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.length);

        Map<UUID, TenantProfile> tpById = Arrays.stream(result)
                .collect(Collectors.toMap(tp -> tp.getId().getId(), Function.identity()));

        for (TenantProfile tp : expected) {
            UUID id = tp.getId().getId();
            TenantProfile found = tpById.get(id);
            Assert.assertNotNull("TenantProfile not found for id " + id, found);

            Assert.assertEquals(tp.getId(), found.getId());
            Assert.assertEquals(tp.getName(), found.getName());
        }
    }


    @Test
    public void testFindDefaultTenantProfileInfo() throws Exception {
        loginSysAdmin();
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals("Default", foundDefaultTenantProfile.getName());
    }

    @Test
    public void testSetDefaultTenantProfile() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile 1");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile defaultTenantProfile = doPost("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString() + "/default", TenantProfile.class);
        Assert.assertNotNull(defaultTenantProfile);
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals(savedTenantProfile.getName(), foundDefaultTenantProfile.getName());
        Assert.assertEquals(savedTenantProfile.getId(), foundDefaultTenantProfile.getId());

        awaitAuditLog("Wait for async audit log to be persisted (UPDATED expected for NEW DEFAULT Tenant Profile)", savedTenantProfile.getId(), ActionType.UPDATED);
    }

    @Test
    public void testSaveTenantProfileWithEmptyName() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = new TenantProfile();
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isBadRequest())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile name " + msgErrorShouldBeSpecified)));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testSaveTenantProfileWithSameName() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isOk());

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile2 = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile with such name already exists")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testDeleteTenantProfileWithExistingTenant() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with tenant profile");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = saveTenant(tenant);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The tenant profile referenced by the tenants cannot be deleted")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testDeleteTenantProfile() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isOk());

        awaitAuditLog("Wait for async audit log to be persisted for deleted tenant profile", savedTenantProfile.getId(), ActionType.DELETED);

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.DELETED, 1);

        doGet("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Tenant profile", savedTenantProfile.getId().getId().toString()))));
    }

    @Test
    public void testFindTenantProfiles() throws Exception {
        loginSysAdmin();
        List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        tenantProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService);

        int cntEntity = 28;
        for (int i = 0; i < 28; i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.CREATED, cntEntity);

        List<TenantProfile> loadedTenantProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                    new TypeReference<>() {}, pageLink);
            loadedTenantProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfiles, idComparator);

        Assert.assertEquals(tenantProfiles, loadedTenantProfiles);

        Mockito.reset(tbClusterService);

        for (TenantProfile tenantProfile : loadedTenantProfiles) {
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<PageData<TenantProfile>>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.DELETED, cntEntity);
    }

    @Test
    public void testFindTenantProfileInfos() throws Exception {
        loginSysAdmin();
        List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> tenantProfilePageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(tenantProfilePageData.hasNext());
        Assert.assertEquals(1, tenantProfilePageData.getTotalElements());
        tenantProfiles.addAll(tenantProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        List<EntityInfo> loadedTenantProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                    new TypeReference<PageData<EntityInfo>>() {}, pageLink);
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

        for (TenantProfile tenantProfile : tenantProfiles) {
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                new TypeReference<PageData<EntityInfo>>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testRateLimitValidationAllFields() throws Exception {
        loginSysAdmin();
        Mockito.reset(tbClusterService);

        List<String> failedFields = new ArrayList<>();

        for (Field field : DefaultTenantProfileConfiguration.class.getDeclaredFields()) {
            RateLimit rateLimit = field.getAnnotation(RateLimit.class);
            if (rateLimit == null) continue;

            String fieldName = field.getName();
            String expectedLabel = rateLimit.fieldName();


            TenantProfile tenantProfile = createTenantProfile("Invalid RateLimit - " + fieldName);
            DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

            field.setAccessible(true);
            field.set(config, "10:1,10:1"); // Set invalid duplicate value

            try {
                doPost("/api/tenantProfile", tenantProfile)
                        .andExpect(status().isBadRequest())
                        .andExpect(statusReason(containsString(expectedLabel + " rate limit has duplicate 'Per seconds' configuration.")));
            } catch (AssertionError e) {
                failedFields.add(fieldName + " (label: " + expectedLabel + ")");
            }
        }

        if (!failedFields.isEmpty()) {
            throw new AssertionError("RateLimit validation failed for fields: " + String.join(", ", failedFields));
        }
        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    private void awaitAuditLog(String awaitMessage, TenantProfileId tenantProfileId, ActionType expectedAction) throws Exception {
        Awaitility.await(awaitMessage)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() ->
                        doGetTypedWithTimePageLink(
                                "/api/audit/logs/entity/TENANT_PROFILE/" + tenantProfileId.getId() + "?",
                                new TypeReference<PageData<AuditLog>>() {
                                },
                                new TimePageLink(5))
                                .getData()
                                .stream()
                                .anyMatch(log -> log.getActionType() == expectedAction)
                );

    }

    private TenantProfile createTenantProfile(String name) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);
        return tenantProfile;
    }

    private void addMainQueueConfig(TenantProfile tenantProfile) {
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

    private void testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(TenantProfile tenantProfile, ComponentLifecycleEvent event, int cntTime) {
        ArgumentMatcher<TenantProfile> matcherTenantProfile = cntTime == 1 ? argument -> argument.equals(tenantProfile) :
                argument -> argument.getClass().equals(TenantProfile.class);
        if (ComponentLifecycleEvent.DELETED.equals(event)) {
            Mockito.verify(tbClusterService, times(cntTime)).onTenantProfileDelete(Mockito.argThat(matcherTenantProfile),
                    eq(TbQueueCallback.EMPTY));
            testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        } else {
            Mockito.verify(tbClusterService, times(cntTime)).onTenantProfileChange(Mockito.argThat(matcherTenantProfile),
                    Mockito.isNull());
            TenantProfileId tenantProfileIdId = cntTime == 1 ? tenantProfile.getId() : (TenantProfileId) createEntityId_NULL_UUID(tenantProfile);
            testBroadcastEntityStateChangeEventTime(tenantProfileIdId, null, cntTime);
        }
        Mockito.reset(tbClusterService);
    }

    private void testBroadcastEntityStateChangeEventNeverTenantProfile() {
        Mockito.verify(tbClusterService, never()).onTenantProfileChange(Mockito.any(TenantProfile.class),
                Mockito.isNull());
        testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        Mockito.reset(tbClusterService, auditLogService);
    }
}
