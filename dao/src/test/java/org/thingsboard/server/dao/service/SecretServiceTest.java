// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class SecretServiceTest extends AbstractServiceTest {

    @Autowired
    SecretService secretService;

    @Autowired
    ConverterService converterService;

    @Autowired
    IntegrationService integrationService;

    @Autowired
    TenantProfileService tenantProfileService;

    @MockitoBean
    SecretConfigurationService secretConfigurationService;

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveSecret() {
        String password = "Password";
        Secret secret = constructSecret(tenantId, "Test Secret", password, SecretType.TEXT);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // check secret info
        SecretInfo retrievedInfo = secretService.findSecretInfoById(tenantId, savedSecret.getId());
        assertThat(retrievedInfo).isEqualTo(new SecretInfo(savedSecret));

        // update encrypted value
        savedSecret.setValue("NewPassword");
        savedSecret = secretService.saveSecret(tenantId, savedSecret);
        retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret);
        assertThat(secretService.findSecretById(tenantId, savedSecret.getId())).isNull();
    }

    @Test
    public void testUpdateSecretInfoDescription_thenValueShouldFetchedFromOldSecret() {
        String password = "Password";
        Secret secret = constructSecret(tenantId, "Test Secret", password, SecretType.TEXT);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // check secret info
        SecretInfo retrievedInfo = secretService.findSecretInfoById(tenantId, savedSecret.getId());
        assertThat(retrievedInfo).isEqualTo(new SecretInfo(savedSecret));

        // update description, value should be equal
        retrievedInfo.setDescription("New description for secret");
        Secret updatedSecret = secretService.saveSecret(tenantId, new Secret(retrievedInfo));
        assertThat(savedSecret.getValue()).isEqualTo(updatedSecret.getValue());

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret);
        assertThat(secretService.findSecretById(tenantId, savedSecret.getId())).isNull();
    }

    @Test
    public void testUpdateSecretName_thenReceiveDataValidationException() {
        Secret secret = constructSecret(tenantId, "Test Validation Exception", "Validation", SecretType.TEXT);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        savedSecret.setName("Updated Validation Exception");

        Assertions.assertThrows(DataValidationException.class, () -> secretService.saveSecret(tenantId, savedSecret));
    }

    @Test
    public void testFindSecretByName() {
        String name = "Test Secret Password";
        Secret secret = constructSecret(tenantId, name, "test", SecretType.TEXT);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getName()).isEqualTo(name);

        Secret retrieved = secretService.findSecretByName(tenantId, name);
        assertThat(savedSecret).isEqualTo(retrieved);

        secretService.deleteSecret(tenantId, savedSecret);
    }

    @Test
    public void testGetTenantSecrets() {
        List<Secret> secrets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Secret savedSecret = secretService.saveSecret(tenantId, constructSecret(tenantId, "Name_" + i, "Password", SecretType.TEXT));
            secrets.add(savedSecret);
        }
        PageData<SecretInfo> retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        List<SecretInfo> secretInfos = secrets.stream().map(SecretInfo::new).toList();
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(secretInfos);

        secrets.forEach(secret -> secretService.deleteSecret(tenantId, secret));
        retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        assertThat(retrieved.getData().size()).isEqualTo(0);
    }

    @Test
    public void testDeleteSecret_whenUsedInIntegration_thenReceiveFailureDeleteResult() {
        String secretName = "IntegrationSecret";
        Secret secret = constructSecret(tenantId, secretName, "Password", SecretType.TEXT);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create converter and integration with secret usage in configuration:
        ObjectNode configuration = JacksonUtil.newObjectNode().putObject("metadata").put("password", toSecretPlaceholder(secretName, secret.getType()));
        String integrationName = "My integration";
        createIntegration(integrationName, configuration, createAndGetConvertedId());

        // delete secret
        TbSecretDeleteResult result = secretService.deleteSecret(tenantId, savedSecret);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferences()).containsKey(EntityType.INTEGRATION);
        assertThat(result.getReferences().get(EntityType.INTEGRATION)).isNotEmpty();
        assertThat(integrationName).isEqualTo(result.getReferences().get(EntityType.INTEGRATION).get(0).getName());
    }

    @Test
    public void testSaveSecretWithExceededTextSizeLimit_thenReceiveDataValidationException() {
        String value = "a".repeat(2049);
        Secret secret = constructSecret(tenantId, "Test Secret With Long Value", value, SecretType.TEXT);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class, () -> secretService.saveSecret(tenantId, secret));
        assertThat(exception.getMessage()).contains("Secret value is 2049 characters; exceeds maximum of 2048 characters");
    }

    @Test
    public void testSaveSecretWithExceededFileSizeLimit_thenReceiveDataValidationException() {
        String value = "a".repeat(512 * 1024 + 1);

        Secret secret = constructSecret(tenantId, "Test Secret With Large File", value, SecretType.TEXT_FILE);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class, () -> secretService.saveSecret(tenantId, secret));
        assertThat(exception.getMessage()).contains("Secret file size is " + value.length() + " bytes; exceeds the maximum of " + (512 * 1024) + " bytes");
    }

    private Secret constructSecret(TenantId tenantId, String name, String value, SecretType secretType) {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(name);
        secret.setType(secretType);
        secret.setValue(value);
        return secret;
    }

    private Integration createIntegration(String name, JsonNode configuration, ConverterId converterId) {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName(name);
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.MQTT);
        integration.setConfiguration(configuration);
        return integrationService.saveIntegration(integration);
    }

    private ConverterId createAndGetConvertedId() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(JacksonUtil.newObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};"));
        return converterService.saveConverter(converter).getId();
    }

    private String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}
