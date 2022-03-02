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
package org.thingsboard.server.service.query.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntitiesSearchRequest;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntitySearchResult;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.query.EntitiesSearchServiceImpl;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EntitiesSearchServiceSqlTest extends AbstractControllerTest {
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DashboardDao dashboardDao;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private RuleChainDao ruleChainDao;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
    }

    @Test
    public void testSearchAllEntityTypes() throws Exception {
        List<String> fields = EntitiesSearchServiceImpl.ENTITY_RESPONSE_FIELDS.stream()
                .map(EntityKey::getKey)
                .collect(Collectors.toList());
        fields.add(EntityKeyMapping.ID);
        fields.add(EntityKeyMapping.LAST_ACTIVITY_TIME);

        for (EntityType entityType : EntitiesSearchServiceImpl.SEARCHABLE_ENTITY_TYPES) {
            for (String sortProperty : fields) {
                for (String sortDirection : Set.of("ASC", "DESC")) {
                    searchEntities(entityType, "query", sortProperty, sortDirection);
                }
            }
        }
    }

    @Test
    public void testSearchDevices() throws Exception {
        String searchQuery = "tyuiop";
        String name = "qwer" + searchQuery + "asdfghjkl";

        Device device = new Device();
        device.setName(name);
        device.setType("profile");
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        Device savedDevice = deviceService.saveDevice(device);

        PageData<EntitySearchResult> searchResult = searchEntities(EntityType.DEVICE, searchQuery, "id", "ASC");
        validateSearchResult(searchResult, savedDevice, deviceSearchResult -> {
            return deviceSearchResult.getType().equals(savedDevice.getType()) &&
                    deviceSearchResult.getOwnerInfo().getId().equals(savedDevice.getCustomerId()) &&
                    deviceSearchResult.getOwnerInfo().getName().equals(customerDao.findById(TenantId.SYS_TENANT_ID, savedDevice.getCustomerId().getId()).getTitle());
        });

        assertThat(searchEntities(EntityType.DEVICE, "noSuchDevice", "id", "ASC").getTotalElements()).isZero();
    }

    @Test
    public void testSearchDashboards() throws Exception {
        String searchQuery = "tyuiop";
        String name = "qwer" + searchQuery + "asdfghjkl";

        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(name);
        Dashboard savedDashboard = dashboardDao.save(tenantId, dashboard);

        PageData<EntitySearchResult> searchResult = searchEntities(EntityType.DASHBOARD, searchQuery, "id", "ASC");

        validateSearchResult(searchResult, savedDashboard);
    }

    @Test
    public void testSearchRuleChains() throws Exception {
        String searchQuery = "tyuiop";
        String name = "qwer" + searchQuery + "asdfghjkl";

        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        RuleChain savedRuleChain = ruleChainDao.save(tenantId, ruleChain);

        PageData<EntitySearchResult> searchResult = searchEntities(EntityType.RULE_CHAIN, searchQuery, "id", "ASC");

        validateSearchResult(searchResult, savedRuleChain, ruleChainSearchResult -> {
            return ruleChainSearchResult.getType().equals(savedRuleChain.getType().name());
        });
    }

    @Test
    public void testSearchUsers_lastActivityTime() throws Exception {
        String searchQuery = "qwerty";

        User user = new User();
        user.setEmail(searchQuery + "@du.ba");
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        String password = "pswrd1231";

        User savedUser1 = createUser(user, password);
        long logInTime1 = System.currentTimeMillis();
        login(savedUser1.getEmail(), password);

        loginSysAdmin();
        user.setEmail(searchQuery + "@ba.du");

        User savedUser2 = createUser(user, password);
        long logInTime2 = System.currentTimeMillis();
        login(savedUser2.getEmail(), password);

        loginSysAdmin();

        List<EntitySearchResult> users = searchEntities(EntityType.USER, searchQuery, EntityKeyMapping.LAST_ACTIVITY_TIME, "DESC").getData();

        assertThat(users).hasSize(2);
        assertThat(users.get(0)).matches(firstItem -> {
            return firstItem.getId().equals(savedUser2.getId()) && firstItem.getLastActivityTime() >= logInTime2;
        });
        assertThat(users.get(1)).matches(secondItem -> {
            return secondItem.getId().equals(savedUser1.getId()) && secondItem.getLastActivityTime() >= logInTime1;
        });

    }

    @Test
    public void testSearchUnsupportedEntityType() {
        assertThrows(Exception.class, () -> {
            searchEntities(EntityType.RULE_NODE, "", "id", "ASC");
        });
    }


    private PageData<EntitySearchResult> searchEntities(EntityType entityType, String searchQuery, String sortProperty, String sortOrder) throws Exception {
        EntitiesSearchRequest searchRequest = new EntitiesSearchRequest();
        searchRequest.setEntityType(entityType);
        searchRequest.setSearchQuery(searchQuery);
        return readResponse(doPost("/api/entities/search?page=0&pageSize=99999&sortProperty=" + sortProperty + "&sortOrder=" + sortOrder, searchRequest)
                .andExpect(status().isOk()), new TypeReference<PageData<EntitySearchResult>>() {
        });
    }


    private <E extends HasId & HasName & HasTenantId> void validateSearchResult(PageData<EntitySearchResult> searchResult, E savedEntity, Predicate<EntitySearchResult> additionalAssert) {
        System.out.println(searchResult.getData().get(0) + "\t" + savedEntity);
        assertThat(searchResult.getTotalElements()).isOne();
        assertThat(searchResult.getData().get(0)).matches(entitySearchResult -> {
            return matches(entitySearchResult, savedEntity) && additionalAssert.test(entitySearchResult);
        });
    }

    private <E extends HasId & HasName & HasTenantId> void validateSearchResult(PageData<EntitySearchResult> searchResult, E savedEntity) {
        validateSearchResult(searchResult, savedEntity, r -> true);
    }

    private <E extends HasId & HasName & HasTenantId> boolean matches(EntitySearchResult searchResult, E entity) {
        return searchResult.getId().equals(entity.getId()) &&
                searchResult.getName().equals(entity.getName()) &&
                searchResult.getTenantInfo().getId().equals(entity.getTenantId()) &&
                searchResult.getTenantInfo().getName().equals(tenantDao.findById(TenantId.SYS_TENANT_ID, entity.getTenantId().getId()).getTitle());
    }

}
