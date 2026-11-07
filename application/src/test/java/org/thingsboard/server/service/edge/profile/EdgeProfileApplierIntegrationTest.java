// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@DaoSqlTest
public class EdgeProfileApplierIntegrationTest extends AbstractControllerTest {

    @MockitoSpyBean
    RelationService relationService;

    @Autowired
    EdgeProfileApplier edgeProfileApplier;

    private EdgeProfile profile;
    private Edge held;
    private Edge other;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        RuleChain root = new RuleChain();
        root.setName("Lock root");
        root.setType(RuleChainType.EDGE);
        root = doPost("/api/ruleChain", root, RuleChain.class);
        EdgeProfile newProfile = new EdgeProfile();
        newProfile.setName("Lock profile");
        newProfile.getProfileData().getRuleChainIds().add(root.getId());
        newProfile.setRootRuleChainId(root.getId());
        profile = doPost("/api/edgeProfile", newProfile, EdgeProfile.class);
        held = saveEdgeOnProfile("Lock held edge");
        other = saveEdgeOnProfile("Lock other edge");
    }

    private Edge saveEdgeOnProfile(String name) throws Exception {
        Edge edge = constructEdge(name, null);
        edge.setEdgeProfileId(profile.getId());
        return doPost("/api/edge", edge, Edge.class);
    }

    @Test
    public void workOnAnEdgeWaitsForTheWorkAlreadyHoldingItWhileOtherEdgesProceed() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            holding.countDown();
            release.await(20, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(relationService).findByFrom(eq(tenantId), eq(held.getId()), eq(RelationTypeGroup.EDGE));

        CompletableFuture<Boolean> first = reapplyAsync(held);
        Assert.assertTrue(holding.await(20, TimeUnit.SECONDS));

        CompletableFuture<Edge> switchOnHeldEdge = CompletableFuture.supplyAsync(() -> {
            try {
                return edgeProfileApplier.applyToEdge(tenantId, held.getId(), false, true, null);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
        Assert.assertTrue(reapplyAsync(other).get(20, TimeUnit.SECONDS));
        assertThatThrownBy(() -> switchOnHeldEdge.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

        release.countDown();
        Assert.assertTrue(first.get(20, TimeUnit.SECONDS));
        Assert.assertEquals(held.getId(), switchOnHeldEdge.get(20, TimeUnit.SECONDS).getId());
    }

    private CompletableFuture<Boolean> reapplyAsync(Edge edge) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return edgeProfileApplier.reapply(tenantId, edge.getId(), profile.getId());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

}
