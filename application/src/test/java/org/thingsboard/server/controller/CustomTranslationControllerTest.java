// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CustomTranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";
    private static final byte[] CUSTOM_TRANSLATION_BYTES = Base64.getDecoder().decode("eyJzYXZlIjoic2F2ZSBhbGFybSJ9");

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
    public void shouldSaveCustomTranslation() throws Exception {
        loginSysAdmin();
        checkSaveCustomTranslation(ES_ES);
        String sysAdminEtag = getFullTranslation(ES_ES);

        loginTenantAdmin();
        checkSaveCustomTranslation(ES_ES);
        String tenantEtag = getFullTranslation(ES_ES);

        loginCustomerAdminUser();
        checkSaveCustomTranslation(ES_ES);
        String customerAdminEtag = getFullTranslation(ES_ES);

        loginSubCustomerAdminUser();
        checkSaveCustomTranslation(ES_ES);
        String subCustomerEtag = getFullTranslation(ES_ES);

        //check if full translation modified
        loginSysAdmin();
        assertThat(getFullTranslationResponse(ES_ES, sysAdminEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginTenantAdmin();
        tenantEtag = getFullTranslationResponse(ES_ES, tenantEtag).getHeader("ETag");

        loginCustomerAdminUser();
        customerAdminEtag = getFullTranslationResponse(ES_ES, customerAdminEtag).getHeader("ETag");

        loginSubCustomerAdminUser();
        assertThat(getFullTranslationResponse(ES_ES, subCustomerEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        // check not modified
        loginSysAdmin();
        assertThat(getFullTranslationResponse(ES_ES, sysAdminEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginTenantAdmin();
        assertThat(getFullTranslationResponse(ES_ES, tenantEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginCustomerAdminUser();
        assertThat(getFullTranslationResponse(ES_ES, customerAdminEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginSubCustomerAdminUser();
        assertThat(getFullTranslationResponse(ES_ES, subCustomerEtag).getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        //update system custom translation and check full translation is being modofied everywhere
        loginSysAdmin();
        JsonNode newCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"system\"}");
        doPost("/api/translation/custom/" + ES_ES, newCustomTranslation);

        assertThat(getFullTranslationResponse(ES_ES, sysAdminEtag).getStatus()).isEqualTo(HttpStatus.OK.value());

        loginTenantAdmin();
        assertThat(getFullTranslationResponse(ES_ES, tenantEtag).getStatus()).isEqualTo(HttpStatus.OK.value());

        loginCustomerAdminUser();
        assertThat(getFullTranslationResponse(ES_ES, customerAdminEtag).getStatus()).isEqualTo(HttpStatus.OK.value());

        loginSubCustomerAdminUser();
        assertThat(getFullTranslationResponse(ES_ES, subCustomerEtag).getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void shouldNotSaveCustomTranslationWithKeyThatOverlapsWithDefault() throws Exception {
        loginSysAdmin();
        JsonNode wrongCustomTranslation = JacksonUtil.toJsonNode("{\"admin.home.customKey\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + ES_ES, wrongCustomTranslation).andExpect(status().isBadRequest());

        JsonNode correctCustomTranslation = JacksonUtil.toJsonNode("{\"admin.home\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + ES_ES, correctCustomTranslation).andExpect(status().isOk());
    }

    @Test
    public void shouldGetCorrectMergedCustomTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        updateSpanishCustomTranslation(esCustomTranslation);

        loginTenantAdmin();
        JsonNode esTenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        updateSpanishCustomTranslation(esTenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode esCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        updateSpanishCustomTranslation(esCustomerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode esSubCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        updateSpanishCustomTranslation(esSubCustomerCustomTranslation);

        // get merged customer custom translation
        loginCustomerAdminUser();
        JsonNode mergedEsCustomTranslation = doGet("/api/translation/custom/merged/" + ES_ES, JsonNode.class);
        assertThat(mergedEsCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedEsCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedEsCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedEsCustomTranslation.get("search").asText()).isEqualTo("customer");

        // get merged subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode mergedSubCustomerCustomTranslation = doGet("/api/translation/custom/merged/" + ES_ES, JsonNode.class);
        assertThat(mergedSubCustomerCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedSubCustomerCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedSubCustomerCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedSubCustomerCustomTranslation.get("search").asText()).isEqualTo("subCustomer");
    }

    @Test
    public void shouldPatchCustomTranslation() throws Exception {
        loginSysAdmin();
        checkPatchCustomTranslation();

        loginTenantAdmin();
        checkPatchCustomTranslation();

        loginCustomerAdminUser();
        checkPatchCustomTranslation();

        loginSubCustomerAdminUser();
        checkPatchCustomTranslation();
    }

    @Test
    public void shouldNotPatchTerminalKeyWithObject() throws Exception {
        loginTenantAdmin();
        JsonNode customTranslation = JacksonUtil.toJsonNode("{\"new\":{\"key\":\"testvalue\"}}");
        doPost("/api/translation/custom/" + ES_ES, customTranslation);

        // should not allow update terminal value with object
        JsonNode patch = JacksonUtil.toJsonNode("{\"new.key.key2\":\"new value\"}");
        doPatch("/api/translation/custom/" + ES_ES, patch).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't update terminal key: new.key")));
    }

    @Test
    public void shouldDeleteCustomTranslationKey() throws Exception {
        loginSysAdmin();
        checkDeleteCustomTranslationKey();

        loginTenantAdmin();
        checkDeleteCustomTranslationKey();

        loginCustomerAdminUser();
        checkDeleteCustomTranslationKey();

        loginSubCustomerAdminUser();
        checkDeleteCustomTranslationKey();
    }

    @Test
    public void shouldUploadCustomTranslation() throws Exception {
        loginSysAdmin();
        uploadCustomTranslation("/api/translation/custom/" + ES_ES + "/upload", CUSTOM_TRANSLATION_BYTES);

        JsonNode retrieved = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(retrieved.get("save").asText()).isEqualTo("save alarm");
    }

    @Test
    public void shouldDeleteCustomTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);

        doDelete("/api/translation/custom/" + ES_ES);

        JsonNode retrieved = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(retrieved).isEmpty();
    }

    private void updateSpanishCustomTranslation(JsonNode newCustomTranslation) throws Exception {
        doPost("/api/translation/custom/" + ES_ES, newCustomTranslation);

        JsonNode updatedCustomTranslation = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCustomTranslation).isEqualTo(newCustomTranslation);
    }

    private void checkSaveCustomTranslation(String localeCode) throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + localeCode, esCustomTranslation);

        JsonNode savedCT =  doGet("/api/translation/custom/" + localeCode, JsonNode.class);
        assertThat(savedCT).isEqualTo(esCustomTranslation);
    }

    private String getFullTranslation(String localeCode) throws Exception {
        return doGet("/api/translation/full/" + localeCode)
                .andReturn().getResponse().getHeader("ETag");
    }

    private MockHttpServletResponse getFullTranslationResponse(String localeCode, String etag) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setIfNoneMatch(etag);
        MockHttpServletResponse response = doGet("/api/translation/full/" + localeCode, headers)
                .andReturn().getResponse();
        return response;
    }

    private void checkPatchCustomTranslation() throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"savealarm\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);

        JsonNode savedCT = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(savedCT).isEqualTo(esCustomTranslation);

        //patch with new value for "save.alarm" element
        JsonNode newValueForKeyWithDot = JacksonUtil.toJsonNode("{\"save.alarm\":\"new value\"}");
        doPatch("/api/translation/custom/" + ES_ES, newValueForKeyWithDot);

        JsonNode expectedResult = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"new value\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        JsonNode updatedCT = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCT).isEqualTo(expectedResult);

        JsonNode newValueForKeyWithoutDot = JacksonUtil.toJsonNode("{\"update\":\"update alarm\"}");
        doPatch("/api/translation/custom/" + ES_ES, newValueForKeyWithoutDot);

        JsonNode expectedResult2 = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"new value\"}, \"update\" : \"update alarm\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        JsonNode updatedCT2 = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCT2).isEqualTo(expectedResult2);
    }

    private void checkDeleteCustomTranslationKey() throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"save alarm\", \"device\":\"save device\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);

        JsonNode savedCT = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(savedCT).isEqualTo(esCustomTranslation);

        //delete key "save.alarm"
        doDelete("/api/translation/custom/" + ES_ES + "/" + "save.alarm");

        JsonNode expectedResult = JacksonUtil.toJsonNode("{\"save\":{\"device\":\"save device\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        JsonNode updatedCT = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCT).isEqualTo(expectedResult);

        //delete key "save.device"
        doDelete("/api/translation/custom/" + ES_ES + "/" + "save.device");

        JsonNode expectedResult2 = JacksonUtil.toJsonNode("{\"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        JsonNode updatedCT2 = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCT2).isEqualTo(expectedResult2);

        //delete key "save"
        doDelete("/api/translation/custom/" + ES_ES + "/" + "save");

        JsonNode expectedResult3 = JacksonUtil.toJsonNode("{\"update\" : \"system\", \"remove\" : \"system\", \"search\":\"system\"}");
        JsonNode updatedCT3 = doGet("/api/translation/custom/" + ES_ES, JsonNode.class);
        assertThat(updatedCT3).isEqualTo(expectedResult3);

        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);
    }

    private void uploadCustomTranslation(String url, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", MediaType.APPLICATION_JSON_VALUE, content);
        var request = MockMvcRequestBuilders.multipart(HttpMethod.POST, url).file(file);
        setJwtToken(request);
        mockMvc.perform(request).andExpect(status().isOk());
    }

}
