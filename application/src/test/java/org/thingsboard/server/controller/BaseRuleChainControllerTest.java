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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseRuleChainControllerTest extends AbstractControllerTest {

    private IdComparator<RuleChain> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        Assert.assertNotNull(savedRuleChain);
        Assert.assertNotNull(savedRuleChain.getId());
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());
        savedRuleChain.setName("New RuleChain");
        doPost("/api/ruleChain", savedRuleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertEquals(savedRuleChain.getName(), foundRuleChain.getName());
    }

    @Test
    public void testSaveRuleChainWithViolationOfLengthValidation() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/ruleChain", ruleChain).andExpect(statusReason(containsString("length of name must be equal or less than 255")));
    }

    @Test
    public void testFindRuleChainById() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertNotNull(foundRuleChain);
        Assert.assertEquals(savedRuleChain, foundRuleChain);
    }

    @Test
    public void testDeleteRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        doDelete("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindEdgeRuleChainsByTenantIdAndName() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<RuleChain> edgeRuleChains = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<RuleChain> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        edgeRuleChains.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName("RuleChain " + i);
            ruleChain.setType(RuleChainType.EDGE);
            RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
            edgeRuleChains.add(savedRuleChain);
        }

        List<RuleChain> loadedEdgeRuleChains = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                    new TypeReference<>() {}, pageLink);
            loadedEdgeRuleChains.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgeRuleChains, idComparator);
        Collections.sort(loadedEdgeRuleChains, idComparator);

        Assert.assertEquals(edgeRuleChains, loadedEdgeRuleChains);

        for (RuleChain ruleChain : loadedEdgeRuleChains) {
            if (!ruleChain.isRoot()) {
                doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                        + "/ruleChain/" + ruleChain.getId().getId().toString(), RuleChain.class);
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

}
