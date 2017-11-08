/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin;
import scala.App;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BaseApplicationControllerTest extends AbstractControllerTest {
    private IdComparator<Application> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    private PluginMetaData sysPlugin;
    private PluginMetaData tenantPlugin;
    private static final ObjectMapper mapper = new ObjectMapper();

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

        if(ldapEnabled) {
            createLDAPEntry(tenantAdmin.getEmail(), "testPassword1");
        }
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");


        sysPlugin = new PluginMetaData();
        sysPlugin.setName("Sys plugin");
        sysPlugin.setApiToken("sysplugin");
        sysPlugin.setConfiguration(mapper.readTree("{}"));
        sysPlugin.setClazz(TelemetryStoragePlugin.class.getName());
        sysPlugin = doPost("/api/plugin", sysPlugin, PluginMetaData.class);

        tenantPlugin = new PluginMetaData();
        tenantPlugin.setName("My plugin");
        tenantPlugin.setApiToken("myplugin");
        tenantPlugin.setConfiguration(mapper.readTree("{}"));
        tenantPlugin.setClazz(TelemetryStoragePlugin.class.getName());
        tenantPlugin = doPost("/api/plugin", tenantPlugin, PluginMetaData.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if(ldapEnabled) {
            deleteLDAPEntry(tenantAdmin.getEmail());
        }
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }


    @Test
    public void testSaveApplication() throws Exception {
        Application application = new Application();
        application.setName("My Application");
        application.setDescription("Application Description");

        Application savedApplication = doPost("/api/application", application, Application.class);

        Assert.assertNotNull(savedApplication);
        Assert.assertNotNull(savedApplication.getId());
        Assert.assertTrue(savedApplication.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedApplication.getTenantId());
        Assert.assertNotNull(savedApplication.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedApplication.getCustomerId().getId());
        Assert.assertEquals(application.getName(), savedApplication.getName());
    }

    @Test
    public void testSaveApplicationWithEmptyName() throws Exception {
        Application application = new Application();
        doPost("/api/application", application)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Application name should be specified")));
    }

    @Test
    public void testFindApplicationById() throws Exception {
        Application application = new Application();
        application.setName("My App");

        Application savedApplication = doPost("/api/application", application, Application.class);
        Application foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertNotNull(foundApplication);
        Assert.assertEquals(savedApplication, foundApplication);
    }

    @Test
    public void testDeleteApplication() throws Exception {
        Application application = new Application();
        application.setName("My application");
        Application savedApplicaiton = doPost("/api/application", application, Application.class);

        doDelete("/api/application/"+savedApplicaiton.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/application/"+savedApplicaiton.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignUnassignApplicationToCustomer() throws Exception {
        Application application = new Application();
        application.setName("My application");
        Application savedApplication = doPost("/api/application", application, Application.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Application assignedApplication = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(savedCustomer.getId(), assignedApplication.getCustomerId());

        Application foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(savedCustomer.getId(), foundApplication.getCustomerId());

        Application unassignedApplication =
                doDelete("/api/customer/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedApplication.getCustomerId().getId());

        foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundApplication.getCustomerId().getId());
    }

    @Test
    public void testAssignApplicationToNonExistentCustomer() throws Exception {
        Application application = new Application();
        application.setName("My application");

        Application savedApplication = doPost("/api/application", application, Application.class);

        doPost("/api/customer/" + UUIDs.timeBased().toString()
                + "/application/" + savedApplication.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignApplicationToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("sometenant@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        System.out.println("JetinderSinghRathore"+ldapEnabled);
        if(ldapEnabled) {
            createLDAPEntry(tenantAdmin2.getEmail(), "testPassword1");
        }
        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Application application = new Application();
        application.setName("My application");

        Application savedApplication = doPost("/api/application", application, Application.class);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/application/" + savedApplication.getId().getId().toString())
                .andExpect(status().isForbidden());

        loginSysAdmin();

        if(ldapEnabled) {
            deleteLDAPEntry(tenantAdmin2.getEmail());
        }
        doDelete("/api/tenant/"+savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantApplications() throws Exception {
        List<Application> applications = new ArrayList<>();
        for (int i=0;i<178;i++) {
            Application application = new Application();
            application.setName("Application"+i);

            applications.add(doPost("/api/application", application, Application.class));
        }
        List<Application> loadedApplications = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Application> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/applications?",
                    new TypeReference<TextPageData<Application>>(){}, pageLink);
            loadedApplications.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(applications, idComparator);
        Collections.sort(loadedApplications, idComparator);

        Assert.assertEquals(applications, loadedApplications);
    }

    @Test
    public void testFindTenantApplicationsByName() throws Exception {
        String title1 = "Application title 1";
        List<Application> applicationsTitle1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Application application = new Application();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            application.setName(name);
            applicationsTitle1.add(doPost("/api/application", application, Application.class));
        }
        String title2 = "Application title 2";
        List<Application> applicationsTitle2 = new ArrayList<>();
        for (int i=0;i<75;i++) {
            Application application = new Application();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            application.setName(name);
            applicationsTitle2.add(doPost("/api/application", application, Application.class));
        }

        List<Application> loadedApplicationsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Application> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/applications?",
                    new TypeReference<TextPageData<Application>>(){}, pageLink);
            loadedApplicationsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(applicationsTitle1, idComparator);
        Collections.sort(loadedApplicationsTitle1, idComparator);

        Assert.assertEquals(applicationsTitle1, loadedApplicationsTitle1);

        List<Application> loadedApplicationsTitle2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/applications?",
                    new TypeReference<TextPageData<Application>>(){}, pageLink);
            loadedApplicationsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(applicationsTitle2, idComparator);
        Collections.sort(loadedApplicationsTitle2, idComparator);

        Assert.assertEquals(applicationsTitle2, loadedApplicationsTitle2);

        for (Application application : loadedApplicationsTitle1) {
            doDelete("/api/application/"+application.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/applications?",
                new TypeReference<TextPageData<Application>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Application application : loadedApplicationsTitle2) {
            doDelete("/api/application/"+application.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/applications?",
                new TypeReference<TextPageData<Application>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testAssignUnAssignDashboardToApplication() throws Exception {
        Application application = new Application();
        application.setName("My application");
        Application savedApplication = doPost("/api/application", application, Application.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        String dashboardType = "main";

        Application assignedApplication = doPost("/api/dashboard/"+dashboardType+"/"+savedDashboard.getId().getId().toString()
                +"/application/"+savedApplication.getId().getId().toString(), Application.class);

        Assert.assertEquals(savedDashboard.getId(), assignedApplication.getDashboardId());

        Application foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(savedDashboard.getId(), foundApplication.getDashboardId());

        Application unassignedApplication =
                doDelete("/api/dashboard/"+dashboardType + "/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedApplication.getDashboardId().getId());

        foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundApplication.getDashboardId().getId());
    }

    @Test
    public void testAssignUnAssignMiniDashboardToApplication() throws Exception {
        Application application = new Application();
        application.setName("My application");
        Application savedApplication = doPost("/api/application", application, Application.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        String dashboardType = "mini";

        Application assignedApplication = doPost("/api/dashboard/"+dashboardType+"/"+savedDashboard.getId().getId().toString()
                +"/application/"+savedApplication.getId().getId().toString(), Application.class);

        Assert.assertEquals(savedDashboard.getId(), assignedApplication.getMiniDashboardId());

        Application foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(savedDashboard.getId(), foundApplication.getMiniDashboardId());

        Application unassignedApplication =
                doDelete("/api/dashboard/"+dashboardType + "/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedApplication.getMiniDashboardId().getId());

        foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundApplication.getMiniDashboardId().getId());
    }

    @Test
    public void testAssignRulesToApplication() throws Exception{
        Application application = new Application();
        application.setName("My application");
        Application savedApplication = doPost("/api/application", application, Application.class);

        RuleMetaData rule1 = new RuleMetaData();
        rule1.setName("My Rule1");
        rule1.setPluginToken(tenantPlugin.getApiToken());
        rule1.setFilters(mapper.readTree("[{\"clazz\":\"org.thingsboard.server.extensions.core.filter.MsgTypeFilter\", " +
                "\"name\":\"TelemetryFilter\", " +
                "\"configuration\": {\"messageTypes\":[\"POST_TELEMETRY\",\"POST_ATTRIBUTES\",\"GET_ATTRIBUTES\"]}}]"));
        rule1.setAction(mapper.readTree("{\"clazz\":\"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction\", \"name\":\"TelemetryMsgConverterAction\", \"configuration\":{\"timeUnit\":\"DAYS\", \"ttlValue\":1}}"));
        RuleMetaData savedRule1 = doPost("/api/rule", rule1, RuleMetaData.class);

        RuleMetaData rule2 = new RuleMetaData();
        rule2.setName("My Rule2");
        rule2.setPluginToken(tenantPlugin.getApiToken());
        rule2.setFilters(mapper.readTree("[{\"clazz\":\"org.thingsboard.server.extensions.core.filter.MsgTypeFilter\", " +
                "\"name\":\"TelemetryFilter\", " +
                "\"configuration\": {\"messageTypes\":[\"POST_TELEMETRY\",\"POST_ATTRIBUTES\",\"GET_ATTRIBUTES\"]}}]"));
        rule2.setAction(mapper.readTree("{\"clazz\":\"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction\", \"name\":\"TelemetryMsgConverterAction\", \"configuration\":{\"timeUnit\":\"DAYS\", \"ttlValue\":1}}"));
        RuleMetaData savedRule2 = doPost("/api/rule", rule2, RuleMetaData.class);

        ApplicationRulesWrapper applicationRulesWrapper = new ApplicationRulesWrapper();
        applicationRulesWrapper.setApplicationId(savedApplication.getId().getId().toString());
        applicationRulesWrapper.setRules(Arrays.asList(savedRule1.getId().getId().toString(), savedRule2.getId().getId().toString()));

        Application assignedApplication = doPostWithDifferentResponse("/api/app/assignRules", applicationRulesWrapper, Application.class);
        Assert.assertEquals(Arrays.asList(savedRule1.getId(), savedRule2.getId()), assignedApplication.getRules());

        Application foundApplication = doGet("/api/application/" + savedApplication.getId().getId().toString(), Application.class);
        Assert.assertEquals(Arrays.asList(savedRule1.getId(), savedRule2.getId()), foundApplication.getRules());


        ApplicationRulesWrapper newApplicationRulesWrapper = new ApplicationRulesWrapper();
        newApplicationRulesWrapper.setApplicationId(savedApplication.getId().getId().toString());
        newApplicationRulesWrapper.setRules(Arrays.asList(savedRule2.getId().getId().toString()));
        Application unAssignedOneRuleApplication = doPostWithDifferentResponse("/api/app/assignRules", newApplicationRulesWrapper, Application.class);
        Assert.assertEquals(Arrays.asList(savedRule2.getId()), unAssignedOneRuleApplication.getRules());
    }


}
