/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.MailOauth2Provider;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class TbMailSender extends JavaMailSenderImpl {
    public static final String MAIL_PROP = "mail.";
    public static final int AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 90;
    private final TbMailContextComponent ctx;
    private Boolean oauth2Enabled;

    public TbMailSender(TbMailContextComponent ctx, JsonNode jsonConfig) {
        super();
        this.ctx = ctx;
        setHost(jsonConfig.get("smtpHost").asText());
        setPort(parsePort(jsonConfig.get("smtpPort").asText()));
        setUsername(jsonConfig.get("username").asText());
        if (jsonConfig.has("password")) {
            setPassword(jsonConfig.get("password").asText());
        }
        setJavaMailProperties(createJavaMailProperties(jsonConfig));
    }

    @SneakyThrows
    @Override
    public void doSend(MimeMessage[] mimeMessages, @Nullable Object[] originalMessages) {
        if (oauth2Enabled){
            setPassword(getAccessToken());
        }
        super.doSend(mimeMessages, originalMessages);
    }

    private Properties createJavaMailProperties(JsonNode jsonConfig) {
        Properties javaMailProperties = new Properties();
        String protocol = jsonConfig.get("smtpProtocol").asText();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
        boolean enableTls = false;
        if (jsonConfig.has("enableTls")) {
            if (jsonConfig.get("enableTls").isBoolean() && jsonConfig.get("enableTls").booleanValue()) {
                enableTls = true;
            } else if (jsonConfig.get("enableTls").isTextual()) {
                enableTls = "true".equalsIgnoreCase(jsonConfig.get("enableTls").asText());
            }
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
        if (enableTls && jsonConfig.has("tlsVersion") && !jsonConfig.get("tlsVersion").isNull()) {
            String tlsVersion = jsonConfig.get("tlsVersion").asText();
            if (StringUtils.isNoneEmpty(tlsVersion)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", tlsVersion);
            }
        }

        boolean enableProxy = jsonConfig.has("enableProxy") && jsonConfig.get("enableProxy").asBoolean();

        if (enableProxy) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", jsonConfig.get("proxyHost").asText());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", jsonConfig.get("proxyPort").asText());
            String proxyUser = jsonConfig.get("proxyUser").asText();
            if (StringUtils.isNoneEmpty(proxyUser)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", proxyUser);
            }
            String proxyPassword = jsonConfig.get("proxyPassword").asText();
            if (StringUtils.isNoneEmpty(proxyPassword)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", proxyPassword);
            }
        }

        oauth2Enabled = jsonConfig.has("enableOauth2") && jsonConfig.get("enableOauth2").asBoolean();
        if (oauth2Enabled) {
            javaMailProperties.put(MAIL_PROP + protocol + ".auth.mechanisms", "XOAUTH2");
        }
        return javaMailProperties;
    }

    public String getAccessToken() throws ThingsboardException {
        try {
            AdminSettings settings = ctx.getAdminSettingsService().findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            JsonNode jsonValue = settings.getJsonValue();
            String clientId = jsonValue.get("clientId").asText();
            String clientSecret = jsonValue.get("clientSecret").asText();
            String refreshToken = jsonValue.get("refreshToken").asText();
            String tokenUri = jsonValue.get("tokenUri").asText();
            String providerId = jsonValue.get("providerId").asText();

            TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                    new GenericUrl(tokenUri), refreshToken)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();
            if (MailOauth2Provider.MICROSOFT.name().equals(providerId)) {
                ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
                ((ObjectNode)jsonValue).put("expiresIn", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                ctx.getAdminSettingsService().saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
            }
            return tokenResponse.getAccessToken();
        } catch (Exception e) {
            log.warn("Unable to retrieve access token: {}", e.getMessage());
            throw new ThingsboardException("Error while requesting access token" + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    private int parsePort(String strPort) {
        try {
            return Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }
}
