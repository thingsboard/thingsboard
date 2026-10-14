// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.common.util.JacksonUtil;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL"
})
@DaoSqlTest
public class IntegrationPackageExportControllerTest extends AbstractControllerTest {

    private Converter savedConverter;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        Converter converter = new Converter();
        converter.setName("Test uplink converter");
        converter.setType(ConverterType.UPLINK);
        converter.setIntegrationType(IntegrationType.OCEANCONNECT);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .put("decoder", "return {deviceName: 'Device A', deviceType: 'sensor'};"));
        savedConverter = doPost("/api/converter", converter, Converter.class);
    }

    @After
    public void afterEach() throws Exception {
        loginSysAdmin();
    }

    @Test
    public void exportPackage_returns_zip_with_integration_and_form_json() throws Exception {
        loginTenantAdmin();

        Integration integration = new Integration();
        integration.setName("Acme OceanConnect Sensor");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setEnabled(true);
        integration.setRoutingKey("test-routing-key-export");
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setConfiguration(new ObjectMapper().readTree("{ \"metadata\": {} }"));
        Integration saved = doPost("/api/integration", integration, Integration.class);

        MvcResult result = doGet("/api/integration/" + saved.getId().getId() + "/export-package")
                .andExpect(status().isOk())
                .andReturn();
        byte[] zipBytes = result.getResponse().getContentAsByteArray();

        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }
        assertThat(entryNames).contains("integration.json", "uplink.json", "form.json");
        assertThat(entryNames).doesNotContain("downlink.json");
    }
}
