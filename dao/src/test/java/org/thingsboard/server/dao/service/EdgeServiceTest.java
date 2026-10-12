// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class EdgeServiceTest extends AbstractServiceTest {

    @Autowired
    TbTenantProfileCache tbTenantProfileCache;
    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    CustomerService customerService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    EdgeProfileService edgeProfileService;
    @Autowired
    RuleChainService ruleChainService;
    @Autowired
    AttributesService attributesService;

    @MockitoBean
    SecretConfigurationService secretConfigurationService;

    private final IdComparator<Edge> idComparator = new IdComparator<>();

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveEdge() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(edge.getTenantId(), savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        savedEdge.setName("My new edge");

        edgeService.saveEdge(savedEdge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFailedSaveLeavesNoAutoCreatedEdgeProfile() {
        Edge edge = constructEdge("Edge with a missing customer", "typeThatIsAboutToFail");
        edge.setCustomerId(new CustomerId(UUID.randomUUID()));

        Assertions.assertThrows(DataValidationException.class, () -> edgeService.saveEdge(edge));

        Assert.assertNull(edgeProfileService.findEdgeProfileByName(tenantId, "typeThatIsAboutToFail"));
    }

    @Test
    public void testUpdateWithoutProfileOrTypeKeepsTheCurrentProfile() {
        Edge saved = edgeService.saveEdge(constructEdge("Edge that keeps its profile", "typeKept"));
        EdgeProfileId profileId = saved.getEdgeProfileId();
        Assert.assertNotEquals(edgeProfileService.findDefaultEdgeProfile(tenantId).getId(), profileId);

        saved.setEdgeProfileId(null);
        saved.setType(null);
        saved.setName("Edge renamed only");
        Edge updated = edgeService.saveEdge(saved);

        Assert.assertEquals(profileId, updated.getEdgeProfileId());
        edgeService.deleteEdge(tenantId, updated.getId());
    }

    @Test
    public void testSaveEdgeWithEmptyName() {
        Edge edge = new Edge();
        edge.setType("default");
        edge.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgeWithEmptyTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgeWithInvalidTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edge.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgesWithInfiniteMaxEdgeLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxEdges(Long.MAX_VALUE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Edge savedEdge = edgeService.saveEdge(constructEdge("My edge", "default"));
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testSaveEdgesWithMaxEdgeOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxEdges(1).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);
        tbTenantProfileCache.evict(defaultTenantProfile.getId());

        Assert.assertEquals(0, edgeService.countByTenantId(tenantId));

        Edge savedEdge = edgeService.saveEdge(constructEdge("My first edge", "default"));
        Assert.assertEquals(1, edgeService.countByTenantId(tenantId));

        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(constructEdge("My second edge that out of maxEdgeCount limit", "default"));
        });
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFindEdgeById() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNotNull(foundEdge);
        Assert.assertEquals(savedEdge, foundEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFindEdgeTypesByTenantId() throws Exception {
        List<Edge> edges = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Edge edge = constructEdge("My edge B" + i, "typeB");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 7; i++) {
                Edge edge = constructEdge("My edge C" + i, "typeC");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 9; i++) {
                Edge edge = constructEdge("My edge A" + i, "typeA");
                edges.add(edgeService.saveEdge(edge));
            }
            List<EntitySubtype> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId).get();
            Assert.assertNotNull(edgeTypes);
            Assert.assertEquals(3, edgeTypes.size());
            Assert.assertEquals("typeA", edgeTypes.get(0).getType());
            Assert.assertEquals("typeB", edgeTypes.get(1).getType());
            Assert.assertEquals("typeC", edgeTypes.get(2).getType());
        } finally {
            edges.forEach((edge) -> {
                edgeService.deleteEdge(tenantId, edge.getId());
            });
        }
    }

    @Test
    public void testConcurrentSaveEdgeWithNewTypeCreatesOneProfile() throws Exception {
        int threads = 6;
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Edge> saved = new ArrayList<>();
        try {
            List<Callable<Edge>> racers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                racers.add(() -> {
                    startTogether.await();
                    return edgeService.saveEdge(constructEdge("Raced edge " + index, "Raced type"));
                });
            }
            for (Future<Edge> future : pool.invokeAll(racers)) {
                // a losing save used to fail the whole request on the edge profile unique constraint
                saved.add(future.get());
            }
            Assert.assertEquals(threads, saved.size());
            saved.forEach(Assert::assertNotNull);
            EdgeProfileId profileId = saved.get(0).getEdgeProfileId();
            Assert.assertNotNull(profileId);
            saved.forEach(edge -> Assert.assertEquals("every edge must end up on the same profile", profileId, edge.getEdgeProfileId()));

            EdgeProfile profile = edgeProfileService.findEdgeProfileByName(tenantId, "Raced type");
            Assert.assertNotNull(profile);
            Assert.assertEquals(profileId, profile.getId());
        } finally {
            pool.shutdownNow();
            saved.forEach(edge -> {
                edgeService.deleteEdge(tenantId, edge.getId());
            });
        }
    }

    @Test
    public void testDeleteEdge() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNotNull(foundEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
        foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNull(foundEdge);
    }

    @Test
    public void testFindEdgesByTenantId() {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Edge edge = constructEdge(tenantId, "Edge " + i, "default");
            edges.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        Assert.assertEquals(edges, loadedEdges);

        edgeService.deleteEdgesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindEdgesByTenantIdAndName() {
        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle2.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            loadedEdgesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle1, idComparator);
        Collections.sort(loadedEdgesTitle1, idComparator);

        Assert.assertEquals(edgesTitle1, loadedEdgesTitle1);

        List<Edge> loadedEdgesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        for (Edge edge : loadedEdgesTitle1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindEdgesByTenantIdAndType() {
        String title1 = "Edge title 1";
        String type1 = "typeA";
        List<Edge> edgesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            edgesType1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        String type2 = "typeB";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            edgesType2.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0 , title1);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type1, pageLink);
            loadedEdgesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType1, idComparator);
        Collections.sort(loadedEdgesType1, idComparator);

        Assert.assertEquals(edgesType1, loadedEdgesType1);

        List<Edge> loadedEdgesType2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type2, pageLink);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType2, idComparator);
        Collections.sort(loadedEdgesType2, idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    private Edge constructEdge(String name, String type) {
        return constructEdge(tenantId, name, type);
    }

    @Test
    public void testFindActiveEdges() throws ExecutionException, InterruptedException {
        Edge edge = edgeService.saveEdge(constructEdge("Active edge", "default"));
        KvEntry activeEntry = new BooleanDataEntry("active", true);
        AttributeKvEntry attribute = new BaseAttributeKvEntry(activeEntry, System.currentTimeMillis());
        attributesService.save(tenantId, edge.getId(), AttributeScope.SERVER_SCOPE, Collections.singletonList(attribute)).get();

        PageData<Edge> activeEdges = edgeService.findActiveEdges(new PageLink(100));

        Assert.assertTrue(activeEdges.getData().stream().anyMatch(e -> e.getId().equals(edge.getId())));
    }

    @Test
    public void testCleanCacheIfEdgeRenamed() {
        String edgeNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String edgeNameAfterRename = StringUtils.randomAlphanumeric(15);

        Edge edge = constructEdge(tenantId, edgeNameBeforeRename, "default");
        edgeService.saveEdge(edge);

        Edge savedEdge = edgeService.findEdgeByTenantIdAndName(tenantId, edgeNameBeforeRename);

        savedEdge.setName(edgeNameAfterRename);
        edgeService.saveEdge(savedEdge);

        Edge renamedEdge = edgeService.findEdgeByTenantIdAndName(tenantId, edgeNameBeforeRename);

        Assert.assertNull("Can't find edge by name in cache if it was renamed", renamedEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFindMissingToRelatedRuleChains() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #1");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain1 = ruleChainService.saveRuleChain(ruleChain);

        ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #2");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain2 = ruleChainService.saveRuleChain(ruleChain);

        ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #3");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain3 = ruleChainService.saveRuleChain(ruleChain);

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Input rule node 1");
        ruleNode1.setType("org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("ruleChainId", ruleChain1.getUuidId().toString());
        ruleNode1.setConfiguration(configuration);

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Input rule node 2");
        ruleNode2.setType("org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        configuration = JacksonUtil.newObjectNode();
        configuration.put("ruleChainId", ruleChain2.getUuidId().toString());
        ruleNode2.setConfiguration(configuration);

        RuleChainMetaData ruleChainMetaData3 = new RuleChainMetaData();
        ruleChainMetaData3.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        ruleChainMetaData3.setFirstNodeIndex(0);
        ruleChainMetaData3.setRuleChainId(ruleChain3.getId());
        ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData3, Function.identity());

        ruleChainService.assignRuleChainToEdge(tenantId, ruleChain3.getId(), savedEdge.getId());

        String missingToRelatedRuleChains = edgeService.findMissingToRelatedRuleChains(tenantId,
                savedEdge.getId(),
                "org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        Assert.assertEquals("{\"Rule Chain #3\":[\"Rule Chain #1\",\"Rule Chain #2\"]}", missingToRelatedRuleChains);
    }

    @Test
    public void testFindEdgesByTenantProfileId() {
        Tenant tenant1 = createTenant();
        Tenant tenant2 = createTenant();
        Assert.assertNotNull(tenant1);
        Assert.assertNotNull(tenant2);

        Edge edge1 = constructEdge(tenant1.getId(), "Tenant1 edge", "default");
        Edge edge2 = constructEdge(tenant2.getId(), "Tenant2 edge", "default");
        Edge savedEdge1 = edgeService.saveEdge(edge1);
        Edge savedEdge2 = edgeService.saveEdge(edge2);
        Assert.assertNotNull(savedEdge1);
        Assert.assertNotNull(savedEdge2);
        Assert.assertEquals(tenant1.getTenantProfileId(), tenant2.getTenantProfileId());

        PageData<Edge> edgesPageData = edgeService.findEdgesByTenantProfileId(tenant2.getTenantProfileId(),
                new PageLink(1000));
        Assert.assertEquals(2, edgesPageData.getTotalElements());
        tenantService.deleteTenant(tenant1.getId());
        tenantService.deleteTenant(tenant2.getId());
    }

    @Test
    public void testSaveEdgeResolvesProfileFromTypeAndDefault() {
        Edge byType = edgeService.saveEdge(constructEdge(tenantId, "Edge typed", "storeType"));
        EdgeProfile typed = edgeProfileService.findEdgeProfileByName(tenantId, "storeType");
        Assert.assertNotNull(typed);
        Assert.assertEquals(typed.getId(), byType.getEdgeProfileId());
        Assert.assertEquals("storeType", byType.getType());

        Edge untyped = constructEdge(tenantId, "Edge untyped", null);
        untyped = edgeService.saveEdge(untyped);
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertEquals(def.getId(), untyped.getEdgeProfileId());
        Assert.assertEquals("default", untyped.getType());

        Edge explicit = constructEdge(tenantId, "Edge explicit", "ignored");
        explicit.setEdgeProfileId(typed.getId());
        explicit = edgeService.saveEdge(explicit);
        Assert.assertEquals("storeType", explicit.getType());
    }

    @Test
    public void testEdgeTypeFollowsProfileRename() {
        Edge edge = edgeService.saveEdge(constructEdge(tenantId, "Edge rename", "renameMe"));
        // Warm the name-keyed cache with the stale type so the post-rename read below proves eviction happened.
        Assert.assertEquals("renameMe", edgeService.findEdgeByTenantIdAndName(tenantId, edge.getName()).getType());

        EdgeProfile profile = edgeProfileService.findEdgeProfileById(tenantId, edge.getEdgeProfileId());
        profile.setName("renamed");
        edgeProfileService.saveEdgeProfile(profile);
        Assert.assertEquals("renamed", edgeService.findEdgeById(tenantId, edge.getId()).getType());
        Assert.assertEquals("renamed", edgeService.findEdgeByTenantIdAndName(tenantId, edge.getName()).getType());
    }

    @Test
    public void testDeleteProfileInUseProhibited() {
        Edge edge = edgeService.saveEdge(constructEdge(tenantId, "Edge in use", "inUse"));
        EdgeProfileId profileId = edge.getEdgeProfileId();
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.deleteEdgeProfile(tenantId, profileId));
        edgeService.deleteEdge(tenantId, edge.getId());
        edgeProfileService.deleteEdgeProfile(tenantId, profileId);
        Assert.assertNull(edgeProfileService.findEdgeProfileById(tenantId, profileId));
    }

    @Test
    public void testProfileRootCopiedOnCreateAndProfileSwitch() throws Exception {
        RuleChain rc1 = saveEdgeRuleChain("Root 1");
        RuleChain rc2 = saveEdgeRuleChain("Root 2");
        RuleChain rc3 = saveEdgeRuleChain("Root 3");
        EdgeProfile p1 = newRootedProfile("P1", rc1);
        EdgeProfile p2 = newRootedProfile("P2", rc2);

        Edge edge = constructEdge(tenantId, "Edge roots", null);
        edge.setEdgeProfileId(p1.getId());
        edge = edgeService.saveEdge(edge);
        Assert.assertEquals(rc1.getId(), edge.getRootRuleChainId());

        // the edge itself may move its root (RuleChainEdgeProcessor does this on an uplink); a later plain cloud-side
        // save must not clobber it with the profile root while the profile is unchanged
        edge = edgeService.setEdgeRootRuleChain(tenantId, edge, rc3.getId());
        Assert.assertEquals(rc3.getId(), edge.getRootRuleChainId());
        edge.setLabel("touched");
        edge = edgeService.saveEdge(edge);
        Assert.assertEquals(rc3.getId(), edge.getRootRuleChainId());

        edge.setEdgeProfileId(p2.getId());
        edge = edgeService.saveEdge(edge);
        Assert.assertEquals(rc2.getId(), edge.getRootRuleChainId());
        Assert.assertEquals("P2", edge.getType());
    }

    private RuleChain saveEdgeRuleChain(String name) {
        RuleChain rc = new RuleChain();
        rc.setTenantId(tenantId);
        rc.setName(name);
        rc.setType(RuleChainType.EDGE);
        return ruleChainService.saveRuleChain(rc);
    }

    private EdgeProfile newRootedProfile(String name, RuleChain root) {
        EdgeProfile p = new EdgeProfile();
        p.setTenantId(tenantId);
        p.setName(name);
        p.getProfileData().getRuleChainIds().add(root.getId());
        p.setRootRuleChainId(root.getId());
        return edgeProfileService.saveEdgeProfile(p);
    }

}
