/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=mock",
})
@Slf4j
public abstract class BaseTenantControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<Tenant>> PAGE_DATA_TENANT_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<TenantInfo>> PAGE_DATA_TENANT_INFO_TYPE_REF = new TypeReference<>() {
    };

    ListeningExecutorService executor;

    @Before
    public void setUp() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testSaveTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");

        Mockito.reset(tbClusterService);

        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        Assert.assertNotNull(savedTenant.getId());
        Assert.assertTrue(savedTenant.getCreatedTime() > 0);
        Assert.assertEquals(tenant.getTitle(), savedTenant.getTitle());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.CREATED, 1);

        savedTenant.setTitle("My new tenant");
        doPost("/api/tenant", savedTenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/" + savedTenant.getId().getId().toString(), Tenant.class);
        Assert.assertEquals(foundTenant.getTitle(), savedTenant.getTitle());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.UPDATED, 1);

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.DELETED, 1);
    }

    @Test
    public void testSaveTenantWithViolationOfValidation() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle(StringUtils.randomAlphanumeric(300));

        Mockito.reset(tbClusterService);

        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgErrorFieldLength("title"))));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    public void testFindTenantById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/" + savedTenant.getId().getId().toString(), Tenant.class);
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(savedTenant, foundTenant);
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantInfoById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        TenantInfo foundTenant = doGet("/api/tenant/info/" + savedTenant.getId().getId().toString(), TenantInfo.class);
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(new TenantInfo(savedTenant, "Default"), foundTenant);
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveTenantWithEmptyTitle() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        Tenant tenant = new Tenant();
        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant title " + msgErrorShouldBeSpecified)));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    public void testSaveTenantWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid email address format")));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    public void testDeleteTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);

        String tenantIdStr = savedTenant.getId().getId().toString();
        doDelete("/api/tenant/" + tenantIdStr)
                .andExpect(status().isOk());
        doGet("/api/tenant/" + tenantIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Tenant", tenantIdStr))));
    }

    @Test
    public void testFindTenants() throws Exception {
        loginSysAdmin();
        List<Tenant> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<Tenant> pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        Mockito.reset(tbClusterService);

        int cntEntity = 56;
        List<ListenableFuture<Tenant>> createFutures = new ArrayList<>(56);
        for (int i = 0; i < cntEntity; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }
        tenants.addAll(Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS));

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(new Tenant(), ComponentLifecycleEvent.CREATED, cntEntity);

        List<Tenant> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(tenants).containsExactlyInAnyOrderElementsOf(loadedTenants);

        deleteEntitiesAsync("/api/tenant/", loadedTenants.stream()
                .filter((t) -> !TEST_TENANT_NAME.equals(t.getTitle()))
                .collect(Collectors.toList()), executor).get(TIMEOUT, TimeUnit.SECONDS);

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(new Tenant(), ComponentLifecycleEvent.DELETED, cntEntity);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }

    @Test
    public void testFindTenantsByTitle() throws Exception {
        log.debug("login sys admin");
        loginSysAdmin();
        log.debug("test started");
        String title1 = "Tenant title 1";
        List<ListenableFuture<Tenant>> createFutures = new ArrayList<>(134);
        for (int i = 0; i < 134; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }

        List<Tenant> tenantsTitle1 = Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("saved '{}', qty {}", title1, tenantsTitle1.size());

        String title2 = "Tenant title 2";
        createFutures = new ArrayList<>(127);
        for (int i = 0; i < 127; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }

        List<Tenant> tenantsTitle2 = Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("saved '{}', qty {}", title2, tenantsTitle2.size());

        List<Tenant> loadedTenantsTitle1 = new ArrayList<>(134);
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Tenant> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenantsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        log.debug("found by name '{}', step 15 {}", title1, loadedTenantsTitle1.size());

        assertThat(tenantsTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedTenantsTitle1);
        log.debug("asserted");

        List<Tenant> loadedTenantsTitle2 = new ArrayList<>(127);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenantsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        log.debug("found by name '{}', step 4 {}", title1, loadedTenantsTitle2.size());
        assertThat(tenantsTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedTenantsTitle2);
        log.debug("asserted");


        deleteEntitiesAsync("/api/tenant/", loadedTenantsTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("deleted '{}', size {}", title1, loadedTenantsTitle1.size());

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        log.debug("tried to search another '{}', step 4", title1);

        deleteEntitiesAsync("/api/tenant/", loadedTenantsTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("deleted '{}', size {}", title2, loadedTenantsTitle2.size());

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        log.debug("tried to search another '{}', step 4", title2);
    }

    @Test
    public void testFindTenantInfos() throws Exception {
        loginSysAdmin();
        List<TenantInfo> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantInfo> pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        List<ListenableFuture<TenantInfo>> createFutures = new ArrayList<>(56);
        for (int i = 0; i < 56; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
            createFutures.add(executor.submit(() ->
                    new TenantInfo(doPost("/api/tenant", tenant, Tenant.class), "Default")));
        }
        tenants.addAll(Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS));

        List<TenantInfo> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(tenants).containsExactlyInAnyOrderElementsOf(loadedTenants);

        deleteEntitiesAsync("/api/tenant/", loadedTenants.stream()
                .filter((t) -> !TEST_TENANT_NAME.equals(t.getTitle()))
                .collect(Collectors.toList()), executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }

    ListenableFuture<List<ResultActions>> deleteTenantsAsync(String urlTemplate, List<Tenant> tenants) {
        List<ListenableFuture<ResultActions>> futures = new ArrayList<>(tenants.size());
        for (Tenant device : tenants) {
            futures.add(executor.submit(() ->
                    doDelete(urlTemplate + device.getId().getId())
                            .andExpect(status().isOk())));
        }
        return Futures.allAsList(futures);
    }

    @Test
    public void testUpdateQueueConfigForIsolatedTenant() throws Exception {
        Comparator<Queue> queueComparator = Comparator.comparing(Queue::getName);
        final String username = "isolatedtenant@thingsboard.org";
        final String password = "123456";
        loginSysAdmin();

        List<Queue> sysAdminQueues;
        PageLink pageLink = new PageLink(10);
        PageData<Queue> pageData;
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {
        }, pageLink);
        sysAdminQueues = pageData.getData();

        Tenant tenant = new Tenant();
        tenant.setTitle("Isolated tenant");
        tenant = doPost("/api/tenant", tenant, Tenant.class);

        User tenantUser = new User();
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setTenantId(tenant.getId());
        tenantUser.setEmail(username);
        createUserAndLogin(tenantUser, password);

        List<Queue> foundTenantQueues;

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(sysAdminQueues, foundTenantQueues);

        loginSysAdmin();

        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("isolated-tb-rule-engine");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile, "Main");
        addQueueConfig(tenantProfile, "Test");
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        tenant.setTenantProfileId(tenantProfile.getId());
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(2, foundTenantQueues.size());

        List<Queue> queuesFromConfig = getQueuesFromConfig(tenantProfile.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        TenantProfile tenantProfile2 = new TenantProfile();
        tenantProfile2.setName("isolated-tb-rule-engine2");
        TenantProfileData tenantProfileData2 = new TenantProfileData();
        tenantProfileData2.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile2.setProfileData(tenantProfileData2);
        tenantProfile2.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile2, "Main");
        addQueueConfig(tenantProfile2, "Test");
        addQueueConfig(tenantProfile2, "Test2");
        tenantProfile2 = doPost("/api/tenantProfile", tenantProfile2, TenantProfile.class);

        tenant.setTenantProfileId(tenantProfile2.getId());
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(3, foundTenantQueues.size());

        queuesFromConfig = getQueuesFromConfig(tenantProfile2.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        tenantProfile2.getProfileData().getQueueConfiguration().removeIf(q -> q.getName().equals("Test"));
        tenantProfile2.getProfileData().getQueueConfiguration().removeIf(q -> q.getName().equals("Test2"));
        addQueueConfig(tenantProfile2, "Test2");
        addQueueConfig(tenantProfile2, "Test3");

        tenantProfile2 = doPost("/api/tenantProfile", tenantProfile2, TenantProfile.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(3, foundTenantQueues.size());

        queuesFromConfig = getQueuesFromConfig(tenantProfile2.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        tenant.setTenantProfileId(null);
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);
        for (Queue queue : foundTenantQueues) {
            doGet("/api/queues/" + queue.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(containsString(msgErrorNotFound)));
        }

        loginSysAdmin();
        doDelete("/api/tenant/" + tenant.getId().getId().toString()).andExpect(status().isOk());
    }

    private void addQueueConfig(TenantProfile tenantProfile, String queueName) {
        TenantProfileQueueConfiguration queueConfiguration = new TenantProfileQueueConfiguration();
        queueConfiguration.setName(queueName);
        queueConfiguration.setTopic("tb_rule_engine." + queueName.toLowerCase());
        queueConfiguration.setPollInterval(25);
        queueConfiguration.setPartitions(1 + new Random().nextInt(99));
        queueConfiguration.setConsumerPerPartition(true);
        queueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(1000);
        queueConfiguration.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(3);
        queueConfiguration.setProcessingStrategy(processingStrategy);
        TenantProfileData profileData = tenantProfile.getProfileData();

        List<TenantProfileQueueConfiguration> configs = profileData.getQueueConfiguration();
        if (configs == null) {
            configs = new ArrayList<>();
        }
        configs.add(queueConfiguration);
        profileData.setQueueConfiguration(configs);
        tenantProfile.setProfileData(profileData);
    }

    private List<Queue> getQueuesFromConfig(List<TenantProfileQueueConfiguration> queueConfiguration, List<Queue> queues) {
        List<Queue> result = new ArrayList<>();
        Map<String, Queue> queueMap = new HashMap<>();
        for (Queue queue : queues) {
            queueMap.put(queue.getName(), queue);
        }

        for (TenantProfileQueueConfiguration config : queueConfiguration) {
            Queue queue = queueMap.get(config.getName());
            if (queue != null) {
                Queue expectedQueue = new Queue(queue.getTenantId(), config);
                expectedQueue.setId(queue.getId());
                expectedQueue.setCreatedTime(queue.getCreatedTime());
                result.add(queue);
            }
        }
        return result;
    }

    private void testBroadcastEntityStateChangeEventTimeManyTimeTenant(Tenant tenant, ComponentLifecycleEvent event, int cntTime) {
        ArgumentMatcher<Tenant> matcherTenant = cntTime == 1 ? argument -> argument.equals(tenant) :
                argument -> argument.getClass().equals(Tenant.class);
        if (ComponentLifecycleEvent.DELETED.equals(event)) {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantDelete(Mockito.argThat(matcherTenant),
                    Mockito.isNull());
        } else {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantChange(Mockito.argThat(matcherTenant),
                    Mockito.isNull());
        }
        TenantId tenantId = cntTime == 1 ? tenant.getId() : (TenantId) createEntityId_NULL_UUID(tenant);
        testBroadcastEntityStateChangeEventTime(tenantId, tenantId,  cntTime);
        Mockito.reset(tbClusterService);
    }

    private void testBroadcastEntityStateChangeEventNeverTenant() {
        Mockito.verify(tbClusterService, never()).onTenantChange(Mockito.any(Tenant.class),
                    Mockito.isNull());
        testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        Mockito.reset(tbClusterService);
    }
}
