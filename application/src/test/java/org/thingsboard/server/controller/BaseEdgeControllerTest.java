/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseEdgeControllerTest extends AbstractControllerTest {

    private IdComparator<Edge> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        savedTenant = doPost("/api/tenant", getNewTenant("My tenant"), Tenant.class);
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
    public void testFindEdgeById() throws Exception {
        Edge savedEdge = getNewSavedEdge("Test edge");
        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertNotNull(foundEdge);
        assertEquals(savedEdge, foundEdge);
    }

    @Test
    public void testSaveEdge() throws Exception {
        Edge savedEdge = getNewSavedEdge("Test edge");

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        assertEquals(savedTenant.getId(), savedEdge.getTenantId());

        savedEdge.setName("New test edge");
        doPost("/api/edge", savedEdge, Edge.class);
        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);

        assertEquals(foundEdge.getName(), savedEdge.getName());
    }

    @Test
    public void testDeleteEdge() throws Exception {
        Edge edge = getNewSavedEdge("Test edge");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        doDelete("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveEdgeWithEmptyName() throws Exception {
        Edge edge = new Edge();
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Edge name should be specified!")));
    }

    @Test
    public void testGetEdges() throws Exception {

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            edges.add(getNewSavedEdge("Test edge " + i));
        }
        List<Edge> loadedEdges = loadListOf(new TextPageLink(23), "/api/edges?");

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        assertEquals(edges, loadedEdges);
    }

    @Test
    public void testGetEdgesByName() throws Exception {
        String name1 = "Entity edge1";
        List<Edge> namesOfEdge1 = fillListOf(143, name1);
        List<Edge> loadedNamesOfEdge1 = loadListOf(new TextPageLink(15, name1), "/api/edges?");
        Collections.sort(namesOfEdge1, idComparator);
        Collections.sort(loadedNamesOfEdge1, idComparator);
        assertEquals(namesOfEdge1, loadedNamesOfEdge1);

        String name2 = "Entity edge2";
        List<Edge> namesOfEdge2 = fillListOf(75, name2);
        List<Edge> loadedNamesOfEdge2 = loadListOf(new TextPageLink(4, name2), "/api/edges?");
        Collections.sort(namesOfEdge2, idComparator);
        Collections.sort(loadedNamesOfEdge2, idComparator);
        assertEquals(namesOfEdge2, loadedNamesOfEdge2);

        for (Edge edge : loadedNamesOfEdge1) {
            doDelete("/api/edge/" + edge.getId().getId().toString()).andExpect(status().isOk());
        }
        TextPageData<Edge> pageData = doGetTypedWithPageLink("/api/edges?",
                new TypeReference<TextPageData<Edge>>() {
                }, new TextPageLink(4, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedNamesOfEdge2) {
            doDelete("/api/edge/" + edge.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink("/api/edges?", new TypeReference<TextPageData<Edge>>() {
        }, new TextPageLink(4, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    private Edge getNewSavedEdge(String name) throws Exception {
        Edge edge = createEdge(name);
        return doPost("/api/edge", edge, Edge.class);
    }

    private Edge createEdge(String name) {
        Edge edge = new Edge();
        edge.setTenantId(savedTenant.getId());
        edge.setName(name);
        return edge;
    }

    private Tenant getNewTenant(String title) {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        return tenant;
    }

    private List<Edge> fillListOf(int limit, String partOfName) throws Exception {
        List<Edge> edgeNames = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String fullName = partOfName + ' ' + RandomStringUtils.randomAlphanumeric(15);
            fullName = i % 2 == 0 ? fullName.toLowerCase() : fullName.toUpperCase();
            Edge edge = getNewSavedEdge(fullName);
            edgeNames.add(doPost("/api/edge", edge, Edge.class));
        }
        return edgeNames;
    }

    private List<Edge> loadListOf(TextPageLink pageLink, String urlTemplate) throws Exception {
        List<Edge> loadedItems = new ArrayList<>();
        TextPageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<TextPageData<Edge>>() {
            }, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }
}
