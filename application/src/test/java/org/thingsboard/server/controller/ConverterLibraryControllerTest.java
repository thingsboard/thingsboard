// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.converter.ConverterLibraryService;
import org.thingsboard.server.service.converter.Model;
import org.thingsboard.server.service.converter.Vendor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "integrations.converters.library.enabled=true"
})
public class ConverterLibraryControllerTest extends AbstractControllerTest {

    @Autowired
    ConverterLibraryService converterLibraryService;

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        await("repo initialization").atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !converterLibraryService.getConvertersInfo().isEmpty());
    }

    @Test
    public void testLibrary() throws Exception {
        validateConverters("uplink", "UPLINK");
        validateConverters("downlink", "DOWNLINK");
    }

    private void validateConverters(String converterType, String expectedType) throws Exception {
        Set<String> seenModels = new HashSet<>();
        for (IntegrationType integrationType : IntegrationType.values()) {
            for (int vendorPage = 0; ; vendorPage++) {
                List<Vendor> vendors = doGetTyped(
                        "/api/converter/library/" + integrationType + "/vendors?converterType=" + converterType
                        + "&page=" + vendorPage + "&pageSize=10&loadImages=false",
                        new TypeReference<>() {});
                if (vendors.isEmpty()) {
                    break;
                }

                for (Vendor vendor : vendors) {
                    assertThat(vendor.name()).as(vendor.name() + " vendor name").isNotBlank();

                    for (int modelPage = 0; ; modelPage++) {
                        List<Model> models = doGetTyped(
                                "/api/converter/library/" + integrationType + "/" + vendor.name() + "/models?converterType=" + converterType
                                + "&page=" + modelPage + "&pageSize=10&loadImages=false",
                                new TypeReference<>() {});
                        if (models.isEmpty()) {
                            break;
                        }

                        for (Model model : models) {
                            String modelUrl = integrationType + "/" + vendor.name() + "/" + model.name();
                            assertThat(seenModels.add(modelUrl)).as("duplicate model from pagination: " + modelUrl).isTrue();

                            assertThat(model.name()).as("name for " + modelUrl).isNotBlank();
                            assertThat(model.info().toString()).as("info for " + modelUrl).isNotBlank().isNotEqualTo("{}");

                            ObjectNode converter = doGet("/api/converter/library/" + modelUrl + "/" + converterType, ObjectNode.class);
                            if (converter.isEmpty()) {
                                return;
                            }
                            assertThat(converter.get("type").asText()).as(modelUrl + " " + converterType + " converter type").isEqualTo(expectedType);

                            ObjectNode converterMetadata = doGet("/api/converter/library/" + modelUrl + "/" + converterType + "/metadata", ObjectNode.class);
                            assertThat(converterMetadata).as(converterType + " converter metadata for " + modelUrl).isNotEmpty();
                            assertThat(converterMetadata.has("integrationName")).as(converterType + " converter metadata integrationName for " + modelUrl).isTrue();

                            String payload = doGet("/api/converter/library/" + modelUrl + "/" + converterType + "/payload", String.class);
                            assertThat(payload).as(converterType + " payload for " + modelUrl).isNotBlank().isNotEqualTo("{}");
                        }
                    }
                }
            }
        }
    }

}
