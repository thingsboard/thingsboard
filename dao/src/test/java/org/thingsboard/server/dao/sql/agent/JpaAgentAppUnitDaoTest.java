// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JpaAgentAppUnitDaoTest extends AbstractAgentJpaDaoTest {

    UUID applicationId1;
    AgentApplication application1;

    @Autowired
    private AgentAppUnitDao agentAppUnitDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        agentId1 = Uuids.timeBased();
        agentProfile1 = saveAgentProfile(tenantId1, "AGENT_APP_UNIT_TEST_PROFILE");
        agent1 = saveAgent(agentId1, tenantId1, Uuids.timeBased(), "AGENT_APP_UNIT_TEST");
        application1 = saveApplication("APP_UNIT_TEST");
        applicationId1 = application1.getId().getId();
    }

    @After
    public void tearDown() {
        if (application1 != null) {
            List<AgentAppUnit> units = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
            for (AgentAppUnit unit : units) {
                agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
            }
            agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        }
        if (agent1 != null) {
            agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        }
        if (agentProfile1 != null) {
            agentProfileDao.removeById(TenantId.fromUUID(tenantId1), agentProfile1.getId().getId());
        }
    }

    @Test
    public void testSaveFindByIdFindByAgentApplicationId() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setTenantId(TenantId.fromUUID(tenantId1));
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier("unit-1");
        unit.setType(AgentAppUnitType.CONTAINER);

        AgentAppUnit saved = agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
        assertNotNull(saved.getId());

        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(applicationId1, found.getAgentApplicationId().getId());
        assertEquals("unit-1", found.getIdentifier());
        assertEquals(AgentAppUnitType.CONTAINER, found.getType());

        List<AgentAppUnit> byApp = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(1, byApp.size());
        assertEquals(saved.getId(), byApp.get(0).getId());

        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentAppUnit unit = saveUnit("unit-2", AgentAppUnitType.VOLUME);
        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentApplicationId() {
        saveUnit("u1", AgentAppUnitType.CONTAINER);
        saveUnit("u2", AgentAppUnitType.NETWORK);
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentAppUnitDao.removeByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testRemoveAgentApplicationRemovesAgentAppUnits() {
        saveUnit("cascade1", AgentAppUnitType.CONTAINER);
        saveUnit("cascade2", AgentAppUnitType.VOLUME);
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        application1 = null;

        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testFindByFilterSortsByEveryMappedProperty() {
        saveUnit("unit-sort-b", AgentAppUnitType.CONTAINER);
        saveUnit("unit-sort-a", AgentAppUnitType.VOLUME);

        AgentAppUnitFilter filter = AgentAppUnitFilter.builder()
                .tenantId(TenantId.fromUUID(tenantId1))
                .applicationId(new AgentApplicationId(applicationId1))
                .build();

        for (String sortProperty : List.of("createdTime", "identifier", "type")) {
            for (SortOrder.Direction direction : SortOrder.Direction.values()) {
                PageLink pageLink = new PageLink(100, 0, null, new SortOrder(sortProperty, direction));
                assertEquals("sort by " + sortProperty + " " + direction,
                        2, agentAppUnitDao.findByFilter(filter, pageLink).getTotalElements());
            }
        }

        PageData<AgentAppUnit> ascByIdentifier = agentAppUnitDao.findByFilter(filter,
                new PageLink(100, 0, null, new SortOrder("identifier", SortOrder.Direction.ASC)));
        assertEquals(List.of("unit-sort-a", "unit-sort-b"),
                ascByIdentifier.getData().stream().map(AgentAppUnit::getIdentifier).toList());
    }

    @Test
    public void testFindByFilterRejectsUnmappedSortProperty() {
        saveUnit("unit-bad-sort", AgentAppUnitType.CONTAINER);

        AgentAppUnitFilter filter = AgentAppUnitFilter.builder()
                .tenantId(TenantId.fromUUID(tenantId1))
                .applicationId(new AgentApplicationId(applicationId1))
                .build();
        PageLink pageLink = new PageLink(100, 0, null, new SortOrder("agentApplicationId", SortOrder.Direction.ASC));

        assertThrows(Exception.class, () -> agentAppUnitDao.findByFilter(filter, pageLink));
    }

    private AgentAppUnit saveUnit(String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setTenantId(TenantId.fromUUID(tenantId1));
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
    }
}
