// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_PARAMETER;

@Slf4j
@DaoSqlTest
public class AdminControllerTest extends AbstractControllerTest {
    final JwtSettings defaultJwtSettings = new JwtSettings(9000, 604800, "thingsboard.io", "QmlicmJkZk9tSzZPVFozcWY0Sm94UVhybmtBWXZ5YmZMOUZSZzZvcUFiOVhsb3VHUThhUWJGaXp3UHhtcGZ6Tw==");

    @Test
    public void testFindAdminSettingsByKey() throws Exception {
        loginSysAdmin();
        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("general")))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://localhost:8080")));

        doGet("/api/admin/settings/mail")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("mail")))
                .andExpect(jsonPath("$.jsonValue.smtpProtocol", is("smtp")))
                .andExpect(jsonPath("$.jsonValue.smtpHost", is("localhost")))
                .andExpect(jsonPath("$.jsonValue.smtpPort", is("25")));

        doGet("/api/admin/settings/unknown")
                .andExpect(status().isNotFound());

    }

    @Test
    public void testSaveAdminSettings() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/general", AdminSettings.class);

        JsonNode jsonValue = adminSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://myhost.org")));

        ((ObjectNode) jsonValue).put("baseUrl", "http://localhost:8080");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAdminSettingsWithEmptyKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey(null);
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Key should be specified")));
    }

    @Test
    public void testChangeAdminSettingsKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey("newKey");
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("is prohibited")));
    }

    @Test
    public void testMailOAuth2AuthorizationStoresOnlyInAppPrevUri() throws Exception {
        loginSysAdmin();
        AdminSettings mailSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        JsonNode originalJsonValue = mailSettings.getJsonValue();
        try {
            ObjectNode jsonValue = JacksonUtil.fromString(originalJsonValue.toString(), ObjectNode.class);
            jsonValue.put("clientId", "clientId");
            jsonValue.put("authUri", "https://accounts.google.com/o/oauth2/v2/auth");
            jsonValue.put("redirectUri", "https://thingsboard.io/api/admin/mail/oauth2/code");
            jsonValue.set("scope", JacksonUtil.newArrayNode().add("https://mail.google.com/"));
            mailSettings.setJsonValue(jsonValue);
            doPost("/api/admin/settings", mailSettings, AdminSettings.class);

            Cookie prevUriCookie = doGet("/api/admin/mail/oauth2/authorize?" + PREV_URI_PARAMETER + "=@evil.com/")
                    .andExpect(status().isOk()).andReturn().getResponse().getCookie(PREV_URI_COOKIE_NAME);
            assertThat(prevUriCookie).isNull();

            prevUriCookie = doGet("/api/admin/mail/oauth2/authorize?" + PREV_URI_PARAMETER + "=/settings/outgoing-mail")
                    .andExpect(status().isOk()).andReturn().getResponse().getCookie(PREV_URI_COOKIE_NAME);
            assertThat(prevUriCookie).isNotNull();
            assertThat(prevUriCookie.getValue()).isEqualTo("/settings/outgoing-mail");
        } finally {
            // the mail settings are shared by the whole test context
            AdminSettings currentSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
            currentSettings.setJsonValue(originalJsonValue);
            doPost("/api/admin/settings", currentSettings, AdminSettings.class);
        }
    }

    @Test
    public void testSendTestMail() throws Exception {
        Mockito.doNothing().when(mailService).sendTestMail(any(), anyString());
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        doPost("/api/admin/settings/testMail", adminSettings)
                .andExpect(status().isOk());
        Mockito.verify(mailService).sendTestMail(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testSendTestMailTimeout() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        ObjectNode objectNode = JacksonUtil.fromString(adminSettings.getJsonValue().toString(), ObjectNode.class);

        objectNode.put("smtpHost", "mail.gandi.net");
        objectNode.put("timeout", 1_000);
        objectNode.put("username", "username");
        objectNode.put("password", "password");

        adminSettings.setJsonValue(objectNode);

        doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError());
        Mockito.verify(mailService).sendTestMail(Mockito.any(), Mockito.anyString());
    }

    void resetJwtSettingsToDefault() throws Exception {
        loginSysAdmin();
        doPost("/api/admin/jwtSettings", defaultJwtSettings).andExpect(status().isOk()); // jwt test scenarios are always started from
        loginTenantAdmin();
    }

    @Test
    public void testGetAndSaveDefaultJwtSettings() throws Exception {
        JwtSettings jwtSettings;
        loginSysAdmin();

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        resetJwtSettingsToDefault();
    }

    @Test
    public void testCreateJwtSettings() throws Exception {
        loginSysAdmin();

        JwtSettings jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                RandomStringUtils.randomAlphanumeric(512 / Byte.SIZE).getBytes(StandardCharsets.UTF_8)));

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        doGet("/api/admin/jwtSettings").andExpect(status().isUnauthorized()); //the old JWT token does not work after signing key was changed!

        loginSysAdmin();
        JwtSettings newJwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(newJwtSettings);

        resetJwtSettingsToDefault();
    }

}
