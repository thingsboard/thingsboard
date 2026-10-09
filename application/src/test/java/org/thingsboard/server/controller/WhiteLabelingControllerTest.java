// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.wl.WhiteLabelingDao;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;

@DaoSqlTest
public class WhiteLabelingControllerTest extends AbstractControllerTest {

    @Autowired
    WhiteLabelingDao whiteLabelingDao;

    @After
    public void afterTest() {
        if (tenantId != null) {
            WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, WhiteLabelingType.MAIL_TEMPLATES);
            if (whiteLabelingDao.findById(SYS_TENANT_ID, key) != null) {
                whiteLabelingDao.removeById(SYS_TENANT_ID, key);
            }
        }

        WhiteLabelingCompositeKey sysKey = new WhiteLabelingCompositeKey(SYS_TENANT_ID, WhiteLabelingType.MAIL_TEMPLATES);
        if (whiteLabelingDao.findById(SYS_TENANT_ID, sysKey) != null) {
            whiteLabelingDao.removeById(SYS_TENANT_ID, sysKey);
        }
    }

    @Test
    public void shouldUpdateWhiteLabelParams() throws Exception {
        loginSysAdmin();
        updateAppTitleAndVerify("New title");

        loginTenantAdmin();
        updateAppTitleAndVerify("New title 2");

        loginCustomerAdminUser();
        updateAppTitleAndVerify("New title 3");
    }

    @Test
    public void shouldUpdateLoginWhiteLabelParams() throws Exception {
        loginSysAdmin();
        updateBaseUrlAndVerify("https://domain.com");

        loginTenantAdmin();
        Domain savedTenantDomain = doPost("/api/domain", constructDomain("domain2.com"), Domain.class);
        updateDomainAndVerify(savedTenantDomain);

        loginCustomerAdminUser();
        Domain savedCustomerDomain = doPost("/api/domain", constructDomain("domain3.com"), Domain.class);
        LoginWhiteLabelingParams loginWhiteLabelingParams = updateDomainAndVerify(savedCustomerDomain);

        //check update settings for registered domain should be prohibited
        loginWhiteLabelingParams.setDomainId(savedTenantDomain.getId());
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams)
                .andExpect(status().isForbidden());

        loginDifferentTenant();
        LoginWhiteLabelingParams differentTenantWLParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        differentTenantWLParams.setDomainId(savedTenantDomain.getId());
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams)
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldGetCorrectMergedWhiteLabelParams() throws Exception {
        loginSysAdmin();
        WhiteLabelingParams systemWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String appName = "SystemName";
        String faviconUrl = "http://testUrl";

        systemWhiteLabelParams.setAppTitle(appName);
        systemWhiteLabelParams.setFavicon(new Favicon(faviconUrl));
        doPost("/api/whiteLabel/whiteLabelParams", systemWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams systemMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(systemMergedParams.getAppTitle()).isEqualTo(appName);
        assertThat(systemMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);

        loginTenantAdmin();
        WhiteLabelingParams tenantWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String tenantAppName = "TenantAppName";
        String logoImageUrl = "http://logoImageUrl";
        tenantWhiteLabelParams.setAppTitle(tenantAppName);
        tenantWhiteLabelParams.setLogoImageUrl(logoImageUrl);
        doPost("/api/whiteLabel/whiteLabelParams", tenantWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams tenantMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(tenantMergedParams.getAppTitle()).isEqualTo(tenantAppName);
        assertThat(tenantMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);
        assertThat(tenantMergedParams.getLogoImageUrl()).isEqualTo(logoImageUrl);

        loginCustomerAdminUser();
        WhiteLabelingParams customerWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String customerAppName = "CustomerAppName";
        customerWhiteLabelParams.setAppTitle(customerAppName);
        doPost("/api/whiteLabel/whiteLabelParams", customerWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams customerMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(customerMergedParams.getAppTitle()).isEqualTo(customerAppName);
        assertThat(customerMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);
        assertThat(customerMergedParams.getLogoImageUrl()).isEqualTo(logoImageUrl);
    }

    @Test
    public void shouldGetCorrectMergedLoginWhiteLabelParams() throws Exception {
        loginSysAdmin();
        LoginWhiteLabelingParams systemloginWhiteLabelParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        systemloginWhiteLabelParams.setDarkForeground(true);
        doPost("/api/whiteLabel/loginWhiteLabelParams", systemloginWhiteLabelParams, WhiteLabelingParams.class);

        LoginWhiteLabelingParams mergedLoginWhiteLabelParams = doGet("/api/noauth/whiteLabel/loginWhiteLabelParams", LoginWhiteLabelingParams.class);
        assertThat(mergedLoginWhiteLabelParams.isDarkForeground()).isTrue();
    }

    @Test
    public void shouldUpdateSystemMailTemplates() throws Exception {
        loginSysAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) mailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        JsonNode updatedMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(updatedMailTemplates.get("subject").asText()).isEqualTo(newSubject);
    }

    @Test
    public void shouldUpdateTenantMailTemplates() throws Exception {
        loginTenantAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) mailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        JsonNode updatedMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(updatedMailTemplates.get("subject").asText()).isEqualTo(newSubject);
    }

    @Test
    public void shouldGetSystemMailTemplatesIfTenantOneDoesNotExistsAndSystemDefaultSetToTrue() throws Exception {
        loginSysAdmin();
        JsonNode systemMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) systemMailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", systemMailTemplates, JsonNode.class);

        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, WhiteLabelingType.MAIL_TEMPLATES);
        if (whiteLabelingDao.findById(tenantId, key) != null) {
            whiteLabelingDao.removeById(tenantId, key);
        }

        loginTenantAdmin();
        JsonNode tenantMailTemplates = doGet("/api/whiteLabel/mailTemplates?systemByDefault=true", JsonNode.class);
        assertThat(tenantMailTemplates.get("subject")).isEqualTo(systemMailTemplates.get("subject"));
    }

    @Test
    public void shouldNotSendMailIfMailTemplateContainsVulnerableCodeExecution() throws Exception {
        loginTenantAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        JsonNode badTemplate = JacksonUtil.toJsonNode("{\"subject\":\"Test message from ThingsBoard tenant\",\"body\":\"<#assign ex=\\\"freemarker.template.utility.Execute\\\"?new()> ${ex(\\\"id\\\")}\"}");
        ((ObjectNode) mailTemplates).set("test", badTemplate);
        ((ObjectNode) mailTemplates).put("useSystemMailSettings", false);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(mailTemplates.get("test")).isEqualTo(badTemplate);

        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        ObjectNode objectNode = JacksonUtil.fromString(adminSettings.getJsonValue().toString(), ObjectNode.class);
        objectNode.put("smtpHost", "smtp.gmail.com");
        objectNode.put("smtpPort", "465");
        objectNode.put("smtpProtocol", "smtps");
        objectNode.put("mailFrom", "testMail@gmail.com");
        objectNode.put("timeout", 1_000);
        objectNode.put("username", "username");
        objectNode.put("password", "password");
        adminSettings.setJsonValue(objectNode);

        //send test mail
        MvcResult mvcResult = doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString()).contains("Failed to process mail template: Instantiating freemarker.template.utility.Execute is not allowed in the template for security reasons");

        //update templates
        badTemplate = JacksonUtil.toJsonNode("{\"subject\":\"Test message from ThingsBoard tenant\",\"body\":\"<#assign uri=object?api.class.getResource(\\\"/\\\").toURI()>\"}");
        ((ObjectNode) mailTemplates).set("test", badTemplate);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        //send test mail
        mvcResult = doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString()).contains("Failed to process mail template: Can't use ?api, because the \\\"api_builtin_enabled\\\" configuration setting is false.");
    }

    @Test
    public void updateLoginWhiteLabelParamsForCustomerByTenant() throws Exception {
        loginTenantAdmin();
        Domain savedTenantDomain = doPost("/api/domain", constructDomain("customer-domain.com"), Domain.class);
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + customerId, LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setDomainId(savedTenantDomain.getId());
        loginWhiteLabelingParams.setBaseUrl("https://" + savedTenantDomain.getName());

        doPost("/api/whiteLabel/loginWhiteLabelParams?customerId=" + customerId, loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        LoginWhiteLabelingParams updated = doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + customerId, LoginWhiteLabelingParams.class);
        assertThat(updated).isEqualTo(loginWhiteLabelingParams);

        loginCustomerAdminUser();
        LoginWhiteLabelingParams updated2 = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        assertThat(updated2).isEqualTo(loginWhiteLabelingParams);
    }

    @Test
    public void shouldNotUpdateLoginWhiteLabelParamsForAnotherCustomerByTenant() throws Exception {
        loginTenantAdmin();
        doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + differentTenantCustomerId);

        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        doPost("/api/whiteLabel/loginWhiteLabelParams?customerId=" + differentTenantCustomerId, loginWhiteLabelingParams);
    }

    @Test
    public void updateWhiteLabelParamsForCustomerByTenant() throws Exception {
        loginTenantAdmin();
        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + customerId, WhiteLabelingParams.class);

        String appTile = "customer title";
        whiteLabelingParams.setAppTitle(appTile);

        doPost("/api/whiteLabel/whiteLabelParams?customerId=" + customerId, whiteLabelingParams, WhiteLabelingParams.class);

        WhiteLabelingParams updated = doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + customerId, WhiteLabelingParams.class);
        assertThat(updated).isEqualTo(whiteLabelingParams);

        loginCustomerAdminUser();
        WhiteLabelingParams updated2 = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        assertThat(updated2).isEqualTo(whiteLabelingParams);
    }

    @Test
    public void shouldNotUpdateWhiteLabelParamsForAnotherCustomerByTenant() throws Exception {
        loginTenantAdmin();
        doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + differentTenantCustomerId);

        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        doPost("/api/whiteLabel/currentWhiteLabelParams?customerId=" + differentTenantCustomerId, loginWhiteLabelingParams);
    }

    @Test
    public void testDomainAlwaysInLowerCase() throws Exception {
        loginTenantAdmin();
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        Domain savedTenantDomain = doPost("/api/domain", constructDomain("my-domain.com"), Domain.class);
        loginWhiteLabelingParams.setDomainId(savedTenantDomain.getId());
        loginWhiteLabelingParams.setBaseUrl("https://" + savedTenantDomain.getName());

        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        LoginWhiteLabelingParams updatedLoginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        assertThat(updatedLoginWhiteLabelingParams.getDomainId()).isEqualTo(savedTenantDomain.getId());
    }

    @Test
    public void shouldDeleteLoginWhiteLabelParamsAfterTenantDeletion() throws Exception {
        loginSysAdmin();
        updateBaseUrlAndVerify("https://domain.com");

        loginTenantAdmin();
        Domain savedTenantDomain = doPost("/api/domain", constructDomain("domain2.com"), Domain.class);
        updateDomainAndVerify(savedTenantDomain);

        loginCustomerAdminUser();
        Domain savedCustomerDomain = doPost("/api/domain", constructDomain("domain3.com"), Domain.class);
        updateDomainAndVerify(savedCustomerDomain);

        loginSubCustomerAdminUser();
        Domain savedSubCustomerDomain = doPost("/api/domain", constructDomain("domain4.com"), Domain.class);
        updateDomainAndVerify(savedSubCustomerDomain);

        //delete tenant
        loginSysAdmin();
        deleteTenant(tenantId);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            WhiteLabeling tenantWL = whiteLabelingDao.findById(SYS_TENANT_ID,  new WhiteLabelingCompositeKey(tenantId, WhiteLabelingType.LOGIN));
            assertThat(tenantWL).isNull();
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            WhiteLabeling customerWL = whiteLabelingDao.findById(SYS_TENANT_ID,  new WhiteLabelingCompositeKey(tenantId, customerId, WhiteLabelingType.LOGIN));
            assertThat(customerWL).isNull();
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            WhiteLabeling subCustomerWL = whiteLabelingDao.findById(SYS_TENANT_ID,  new WhiteLabelingCompositeKey(tenantId, subCustomerId, WhiteLabelingType.LOGIN));
            assertThat(subCustomerWL).isNull();
        });
        tenantId = null;
    }

    @Test
    public void shouldGetCorrectMergedOverrideTrendzSettings() throws Exception {
        loginSysAdmin();
        WhiteLabelingParams sysAdminWhitelabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        sysAdminWhitelabelParams.setOverrideTrendzName(true);
        doPost("/api/whiteLabel/whiteLabelParams", sysAdminWhitelabelParams, WhiteLabelingParams.class);

        loginTenantAdmin();
        WhiteLabelingParams tenantWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(tenantWhiteLabelParams.getOverrideTrendzName()).isTrue();

        loginCustomerAdminUser();
        WhiteLabelingParams customerWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(customerWhiteLabelParams.getOverrideTrendzName()).isTrue();

        loginSubCustomerAdminUser();
        WhiteLabelingParams subCustomerWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(subCustomerWhiteLabelParams.getOverrideTrendzName()).isTrue();

        //override tenant value
        loginTenantAdmin();
        tenantWhiteLabelParams.setOverrideTrendzName(false);
        doPost("/api/whiteLabel/whiteLabelParams", tenantWhiteLabelParams, WhiteLabelingParams.class);

        loginCustomerAdminUser();
        customerWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(customerWhiteLabelParams.getOverrideTrendzName()).isFalse();

        loginSubCustomerAdminUser();
        subCustomerWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(subCustomerWhiteLabelParams.getOverrideTrendzName()).isFalse();

        subCustomerWhiteLabelParams.setOverrideTrendzName(true);
        doPost("/api/whiteLabel/whiteLabelParams", subCustomerWhiteLabelParams, WhiteLabelingParams.class);
        subCustomerWhiteLabelParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(subCustomerWhiteLabelParams.getOverrideTrendzName()).isTrue();
    }

    private void updateAppTitleAndVerify(String appTile) throws Exception {

        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        whiteLabelingParams.setAppTitle(appTile);
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);

        Awaitility.await("Waiting while whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> appTile.equals(doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class).getAppTitle()));
    }

    private LoginWhiteLabelingParams updateBaseUrlAndVerify(String baseUrl) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setBaseUrl(baseUrl);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        Awaitility.await("Waiting while login whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> baseUrl.equals(doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class).getBaseUrl()));

        return doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
    }

    private LoginWhiteLabelingParams updateDomainAndVerify(Domain domain) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setDomainId(domain.getId());
        loginWhiteLabelingParams.setBaseUrl("https://" + domain.getName());
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        Awaitility.await("Waiting while login whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> domain.getId().equals(doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class).getDomainId()));

        return doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
    }

    private Domain constructDomain(String domainName) {
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(false);
        return domain;
    }

}
