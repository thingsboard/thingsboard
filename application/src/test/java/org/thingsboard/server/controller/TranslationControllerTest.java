// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@Slf4j
public class TranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";
    private static final String PL_PL = "pl_PL";
    private static final String EN_AU = "en_AU";
    private static final String EN_US = "en_US";
    private static final String AR_QA = "ar_QA";
    private static final String AR_EG = "ar_EG";


    @Autowired
    CustomTranslationService customTranslationService;
    @Autowired
    AdminSettingsDao  adminSettingsDao;

    @After
    public void tearDownCustomTranslation() throws Exception {
        loginSysAdmin();
        List<TranslationInfo> translationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {});
        for (var info : translationInfos) {
            doDelete("/api/translation/custom/" + info.getLocaleCode());
            JsonNode retrieved = doGet("/api/translation/custom/" + info.getLocaleCode(), JsonNode.class);
            assertThat(retrieved).isEmpty();
        }
    }

    @Test
    public void shouldGetCorrectFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode systemCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, systemCustomTranslation);

        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + ES_ES, tenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode customerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + ES_ES, customerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + ES_ES, subCustomerCustomTranslation);

        // get full system translation
        loginSysAdmin();
        JsonNode fullSystemTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullSystemTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullSystemTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullSystemTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Plantilla de solución");

        // get full tenant translation
        loginTenantAdmin();
        JsonNode fullTenantTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullTenantTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullTenantTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullTenantTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullTenantTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Plantilla de solución");

        // get full customer custom translation
        loginCustomerAdminUser();
        JsonNode fullCustomerTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullCustomerTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullCustomerTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullCustomerTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(fullCustomerTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullCustomerTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Plantilla de solución");

        // get full subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode fullSubCustomerTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullSubCustomerTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullSubCustomerTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullSubCustomerTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(fullSubCustomerTranslation.get("search").asText()).isEqualTo("subCustomer");
        assertThat(fullSubCustomerTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullSubCustomerTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Plantilla de solución");
    }

    @Test
    public void shouldGetCorrectFullTranslationWithAddedFromDefaultLocale() throws Exception {
        loginSysAdmin();
        JsonNode systemCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + EN_US, systemCustomTranslation);

        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + EN_US, tenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode customerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + EN_US, customerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + EN_US, subCustomerCustomTranslation);

        // get full system translation
        loginSysAdmin();
        JsonNode plfullSystemTranslation = doGet("/api/translation/full/" + PL_PL, JsonNode.class);
        assertThat(plfullSystemTranslation.get("save").asText()).isEqualTo("system");
        assertThat(plfullSystemTranslation.get("action").get("activate").asText()).isEqualTo("Aktywuj");

        // get full tenant translation
        loginTenantAdmin();
        JsonNode plFullTenantTranslation = doGet("/api/translation/full/" + PL_PL, JsonNode.class);
        assertThat(plFullTenantTranslation.get("save").asText()).isEqualTo("system");
        assertThat(plFullTenantTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(plFullTenantTranslation.get("action").get("activate").asText()).isEqualTo("Aktywuj");

        // get full customer custom translation
        loginCustomerAdminUser();
        JsonNode plFullCustomTranslation = doGet("/api/translation/full/" + PL_PL, JsonNode.class);
        assertThat(plFullCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(plFullCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(plFullCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(plFullCustomTranslation.get("action").get("activate").asText()).isEqualTo("Aktywuj");

        // get full subcustomer custom translation
        loginSubCustomerAdminUser();

        JsonNode plFullSubCustomTranslation = doGet("/api/translation/full/" + PL_PL, JsonNode.class);
        assertThat(plFullSubCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(plFullSubCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(plFullSubCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(plFullSubCustomTranslation.get("search").asText()).isEqualTo("subCustomer");
        assertThat(plFullSubCustomTranslation.get("action").get("activate").asText()).isEqualTo("Aktywuj");
    }

    @Test
    public void shouldGetCorrectTranslationForBasicEdit() throws Exception {
        loginSysAdmin();
        JsonNode systemCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"systemAccount\"}, \"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, systemCustomTranslation);
        JsonNode systemDefaultCustomTranslation = JacksonUtil.toJsonNode("{\"newSystem\":\"newSystemEnglish\"}");
        doPost("/api/translation/custom/" + EN_US, systemDefaultCustomTranslation);

        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"tenantAccount\"}, \"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + ES_ES, tenantCustomTranslation);
        JsonNode tenantDefaultCustomTranslation = JacksonUtil.toJsonNode("{\"newTenant\":\"newTenantEnglish\"}");
        doPost("/api/translation/custom/" + EN_US, tenantDefaultCustomTranslation);

        loginCustomerAdminUser();
        JsonNode customerCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"customerAccount\"}, \"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + ES_ES, customerCustomTranslation);
        JsonNode customerDefaultCustomTranslation = JacksonUtil.toJsonNode("{\"newCustomer\":\"newCustomerEnglish\"}");
        doPost("/api/translation/custom/" + EN_US, customerDefaultCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"subCustomerAccount\"}, \"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + ES_ES, subCustomerCustomTranslation);
        JsonNode subCustomerDefaultCustomTranslation = JacksonUtil.toJsonNode("{\"newSubCustomer\":\"newSubCustomerEnglish\"}");
        doPost("/api/translation/custom/" + EN_US, subCustomerDefaultCustomTranslation);

        // get system translation for edit
        loginSysAdmin();
        JsonNode systemTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(systemTranslationForEdit.get("account").get("account"), "systemAccount", "Account", "Cuenta", "C");
        verifyInfo(systemTranslationForEdit.get("save"), "system", null, null, "A");
        verifyInfo(systemTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(systemTranslationForEdit.get("newSystem"), null, "newSystemEnglish", null, "A");

        // get tenant translation for edit
        loginTenantAdmin();
        JsonNode tenantTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(tenantTranslationForEdit.get("account").get("account"), "tenantAccount", "Account", "systemAccount", "C");
        verifyInfo(tenantTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(tenantTranslationForEdit.get("update"), "tenant", null, "system", "C");
        verifyInfo(tenantTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(tenantTranslationForEdit.get("newSystem"), null, "newSystemEnglish", null, "U");
        verifyInfo(tenantTranslationForEdit.get("newTenant"), null, "newTenantEnglish", null, "A");

        // get customer for edit
        loginCustomerAdminUser();
        JsonNode customerTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(customerTranslationForEdit.get("account").get("account"), "customerAccount", "Account", "tenantAccount", "C");
        verifyInfo(customerTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("update"), "tenant", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("remove"), "customer", null, "tenant", "C");
        verifyInfo(customerTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(customerTranslationForEdit.get("newSystem"), null, "newSystemEnglish", null, "U");
        verifyInfo(customerTranslationForEdit.get("newCustomer"), null, "newCustomerEnglish", null, "A");

        // get subcustomer translation  for edit
        loginSubCustomerAdminUser();
        JsonNode subCustomerTranslation = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(subCustomerTranslation.get("account").get("account"), "subCustomerAccount", "Account", "customerAccount", "C");
        verifyInfo(subCustomerTranslation.get("save"), "system", null, null, "T");
        verifyInfo(subCustomerTranslation.get("update"), "tenant", null, null, "T");
        verifyInfo(subCustomerTranslation.get("remove"), "customer", null, null, "T");
        verifyInfo(subCustomerTranslation.get("search"), "subCustomer", null, "customer", "C");
        verifyInfo(subCustomerTranslation.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(subCustomerTranslation.get("newSystem"), null, "newSystemEnglish", null, "U");
        verifyInfo(subCustomerTranslation.get("newSubCustomer"), null, "newSubCustomerEnglish", null, "A");

        //set value for added key
        loginSysAdmin();
        systemCustomTranslation = JacksonUtil.toJsonNode("{\"newSystem\":\"newSystemES\"}");
        doPatch("/api/translation/custom/" + ES_ES, systemCustomTranslation);

        loginTenantAdmin();
        tenantCustomTranslation = JacksonUtil.toJsonNode("{\"newTenant\":\"newTenantES\"}");
        doPatch("/api/translation/custom/" + ES_ES, tenantCustomTranslation);

        loginCustomerAdminUser();
        customerCustomTranslation = JacksonUtil.toJsonNode("{\"newCustomer\":\"newCustomerES\"}");
        doPatch("/api/translation/custom/" + ES_ES, customerCustomTranslation);

        loginSubCustomerAdminUser();
        subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"newSubCustomer\":\"newSubCustomerES\"}");
        doPatch("/api/translation/custom/" + ES_ES, subCustomerCustomTranslation);

        // get system translation for edit
        loginSysAdmin();
        systemTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(systemTranslationForEdit.get("account").get("account"), "systemAccount", "Account", "Cuenta", "C");
        verifyInfo(systemTranslationForEdit.get("save"), "system", null, null, "A");
        verifyInfo(systemTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(systemTranslationForEdit.get("newSystem"), "newSystemES", "newSystemEnglish", null, "A");

        // get tenant translation for edit
        loginTenantAdmin();
        tenantTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(tenantTranslationForEdit.get("account").get("account"), "tenantAccount", "Account", "systemAccount", "C");
        verifyInfo(tenantTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(tenantTranslationForEdit.get("update"), "tenant", null, "system", "C");
        verifyInfo(tenantTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(tenantTranslationForEdit.get("newSystem"), "newSystemES", "newSystemEnglish", null, "T");
        verifyInfo(tenantTranslationForEdit.get("newTenant"), "newTenantES", "newTenantEnglish", null, "A");

        // get customer for edit
        loginCustomerAdminUser();
        customerTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(customerTranslationForEdit.get("account").get("account"), "customerAccount", "Account", "tenantAccount", "C");
        verifyInfo(customerTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("update"), "tenant", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("remove"), "customer", null, "tenant", "C");
        verifyInfo(customerTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(customerTranslationForEdit.get("newSystem"), "newSystemES", "newSystemEnglish", null, "T");
        verifyInfo(customerTranslationForEdit.get("newCustomer"), "newCustomerES", "newCustomerEnglish", null, "A");

        // get subcustomer translation  for edit
        loginSubCustomerAdminUser();
        subCustomerTranslation = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(subCustomerTranslation.get("account").get("account"), "subCustomerAccount", "Account", "customerAccount", "C");
        verifyInfo(subCustomerTranslation.get("save"), "system", null, null, "T");
        verifyInfo(subCustomerTranslation.get("update"), "tenant", null, null, "T");
        verifyInfo(subCustomerTranslation.get("remove"), "customer", null, null, "T");
        verifyInfo(subCustomerTranslation.get("search"), "subCustomer", null, "customer", "C");
        verifyInfo(subCustomerTranslation.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(subCustomerTranslation.get("newSystem"), "newSystemES", "newSystemEnglish", null, "T");
        verifyInfo(subCustomerTranslation.get("newSubCustomer"), "newSubCustomerES", "newSubCustomerEnglish", null, "A");
    }

    @Test
    public void shouldGetCorrectTranslationForBasicEditForNewLang() throws Exception {
        loginSysAdmin();
        JsonNode arabicTranslation = JacksonUtil.toJsonNode("{\"my-new-key\": \"arabic-version\"}");
        doPost("/api/translation/custom/" + AR_EG, arabicTranslation);
        JsonNode englishTranslation = JacksonUtil.toJsonNode("{\"my-new-key\":\"english-version\"}");
        doPost("/api/translation/custom/" + EN_US, englishTranslation);

        loginTenantAdmin();
        JsonNode tenantTranslationForEdit = doGet("/api/translation/edit/basic/" + AR_EG, JsonNode.class);
        verifyInfo(tenantTranslationForEdit.get("my-new-key"), "arabic-version", "english-version", null, "T");

        // get customer for edit
        loginCustomerAdminUser();
        JsonNode customerTranslationForEdit = doGet("/api/translation/edit/basic/" + AR_EG, JsonNode.class);
        verifyInfo(customerTranslationForEdit.get("my-new-key"), "arabic-version", "english-version", null, "T");

        // get subcustomer translation  for edit
        loginSubCustomerAdminUser();
        JsonNode subCustomerTranslation = doGet("/api/translation/edit/basic/" + AR_EG, JsonNode.class);
        verifyInfo(subCustomerTranslation.get("my-new-key"), "arabic-version", "english-version", null, "T");
    }

    private static void verifyInfo(JsonNode keyInfo, String translated, String origin, String parent, String state) {
        if (translated != null) {
            assertThat(keyInfo.get("t").asText()).isEqualTo(translated);
        }
        if (origin != null) {
            assertThat(keyInfo.get("o").asText()).isEqualTo(origin);
        }
        if (parent != null) {
            assertThat(keyInfo.get("p").asText()).isEqualTo(parent);
        }
        assertThat(keyInfo.get("s").asText()).isEqualTo(state);
    }

    @Test
    public void shouldGetCorrectTranslationInfo() throws Exception {
        loginSysAdmin();
        checkTranslationInfo();

        loginTenantAdmin();
        checkTranslationInfo();

        loginCustomerAdminUser();
        checkTranslationInfo();

        loginSubCustomerAdminUser();
        checkTranslationInfo();
    }

    @Test
    public void shouldCalculateTranslationProgress() throws Exception {
        loginSysAdmin();
        //check progress is 0
        JsonNode customTranslation = JacksonUtil.toJsonNode("{\"save\":\"arabic\"}");
        doPost("/api/translation/custom/" + AR_QA, customTranslation);

        TranslationInfo arabic = getTranslationInfo(AR_QA);
        assertThat(arabic.getProgress()).isEqualTo(0);

        JsonNode fullEngTranslation = doGet("/api/translation/full/" + EN_US, JsonNode.class);

        //translate some keys and check progress > 0
        Iterator<String> fieldNames = fullEngTranslation.fieldNames();
        int count = 20;
        while (fieldNames.hasNext() && count > 0) {
            String fieldName = fieldNames.next();
            ((ObjectNode) customTranslation).set(fieldName, fullEngTranslation.get(fieldName));
            count--;
        }
        doPost("/api/translation/custom/" + AR_QA, customTranslation);

        Set<String> fullTranslationKeys = JacksonUtil.extractKeys(fullEngTranslation);
        Set<String> translated = JacksonUtil.extractKeys(customTranslation);

        TranslationInfo updatedSystemArabicInfo = getTranslationInfo(AR_QA);
        assertThat(updatedSystemArabicInfo.getProgress()).isGreaterThan(0)
                .isEqualTo(((translated.size() - 1)  * 100)/fullTranslationKeys.size()); // we don`t take into account the added key "save"

        //login as tenant, translate all keys, check progress is 100
        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"arabic\"}");
        doPost("/api/translation/custom/" + AR_QA, tenantCustomTranslation);
        TranslationInfo tenantArabicInfo = getTranslationInfo(AR_QA);
        assertThat(tenantArabicInfo.getProgress()).isEqualTo(updatedSystemArabicInfo.getProgress());

        doPost("/api/translation/custom/" + AR_QA, fullEngTranslation);
        TranslationInfo updatedTenantArabic = getTranslationInfo(AR_QA);
        assertThat(updatedTenantArabic.getProgress()).isEqualTo(100);
    }

    @Test
    public void shouldDownloadFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);

        //download full translation
        byte[] contentAsByteArray = doGet("/api/translation/full/" + ES_ES).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        JsonNode downloadedCustomTranslation = JacksonUtil.fromBytes(contentAsByteArray);

        assertThat(downloadedCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(downloadedCustomTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(downloadedCustomTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Plantilla de solución");
    }

    @Test
    public void shouldGetCorrectLoginTranslation() throws Exception {
        // get login system translation
        loginSysAdmin();
        JsonNode loginSystemTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginSystemTranslation.get("login").get("login").asText()).isEqualTo("Iniciar sesión");
        JsonNode enloginSystemTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enloginSystemTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login tenant translation
        loginTenantAdmin();
        JsonNode loginTenantTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginTenantTranslation.get("login").get("login").asText()).isEqualTo("Iniciar sesión");
        JsonNode enloginTenantTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enloginTenantTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login customer custom translation
        loginCustomerAdminUser();
        JsonNode loginCustomerTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginCustomerTranslation.get("login").get("login").asText()).isEqualTo("Iniciar sesión");
        JsonNode enLoginCustomerTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enLoginCustomerTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode loginSubCustomerTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginSubCustomerTranslation.get("login").get("login").asText()).isEqualTo("Iniciar sesión");
        JsonNode enLoginSubCustomerTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enLoginSubCustomerTranslation.get("login").get("login").asText()).isEqualTo("Login");
    }

    @Test
    public void checkLoginTranslationEtag() throws Exception {
        loginSysAdmin();
        updateCustomTranslation(ES_ES);
        String sysAdminEtag = getLoginTranslation(ES_ES);
        assertThat(sysAdminEtag).isNotNull();

        loginTenantAdmin();
        updateCustomTranslation(ES_ES);
        String tenantEtag = getLoginTranslation(ES_ES);
        assertThat(tenantEtag).isNotNull();

        loginCustomerAdminUser();
        updateCustomTranslation(ES_ES);
        String customerAdminEtag = getLoginTranslation(ES_ES);
        assertThat(customerAdminEtag).isNotNull();

        loginSubCustomerAdminUser();
        updateCustomTranslation(ES_ES);
        String subCustomerEtag = getLoginTranslation(ES_ES);
        assertThat(subCustomerEtag).isNotNull();

        // check not modified
        loginSysAdmin();
        assertThat(geLoginTranslationResponse(ES_ES, sysAdminEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginTenantAdmin();
        assertThat(geLoginTranslationResponse(ES_ES, tenantEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginCustomerAdminUser();
        assertThat(geLoginTranslationResponse(ES_ES, customerAdminEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginSubCustomerAdminUser();
        assertThat(geLoginTranslationResponse(ES_ES, subCustomerEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
    }

    private String getLoginTranslation(String localeCode) throws Exception {
        return doGet("/api/noauth/translation/login/" + localeCode)
                .andReturn().getResponse().getHeader("ETag");
    }

    private MockHttpServletResponse geLoginTranslationResponse(String localeCode, String etag) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setIfNoneMatch(etag);
        return doGet("/api/noauth/translation/login/" + localeCode, headers)
                .andReturn().getResponse();
    }

    private void updateCustomTranslation(String localeCode) throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"login.login\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + localeCode, esCustomTranslation);
    }

    private void checkTranslationInfo() throws Exception {
        JsonNode auCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"australian\"}");
        doPost("/api/translation/custom/" + EN_AU, auCustomTranslation);

        List<TranslationInfo> translationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {
        });

        Optional<TranslationInfo> defaultLocaleInfo = translationInfos.stream().filter(info -> EN_US.equalsIgnoreCase(info.getLocaleCode()))
                .findFirst();
        assertThat(defaultLocaleInfo).isPresent();
        TranslationInfo englishInfo = defaultLocaleInfo.get();

        assertThat(englishInfo.getLocaleCode()).isEqualToIgnoringCase(EN_US);
        assertThat(englishInfo.getLanguage()).isEqualToIgnoringCase("English (English)");
        assertThat(englishInfo.getCountry()).isEqualToIgnoringCase("United States");
        assertThat(englishInfo.getProgress()).isEqualTo(100);

        Optional<TranslationInfo> australian = translationInfos.stream().filter(info -> EN_AU.equalsIgnoreCase(info.getLocaleCode()))
                .findFirst();
        assertThat(australian).isPresent();
        TranslationInfo australianInfo = australian.get();

        assertThat(australianInfo.getLocaleCode()).isEqualToIgnoringCase(EN_AU);
        assertThat(australianInfo.getLanguage()).isEqualToIgnoringCase("English (English)");
        assertThat(australianInfo.getCountry()).isEqualToIgnoringCase("Australia");
        assertThat(australianInfo.getProgress()).isEqualTo(0);
    }

    private TranslationInfo getTranslationInfo(String localeCode) throws Exception {
        List<TranslationInfo> translationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {
        });
        Optional<TranslationInfo> translationInfo = translationInfos.stream().filter(info -> info.getLocaleCode().equals(localeCode))
                .findFirst();
        assertThat(translationInfo).isPresent();
        return translationInfo.get();
    }
}
