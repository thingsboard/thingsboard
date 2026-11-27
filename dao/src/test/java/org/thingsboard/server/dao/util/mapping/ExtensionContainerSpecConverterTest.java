// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.util.mapping;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The container_spec column is JSONB. It was written through hypersistence-utils before, so the
 * converter must read the text PostgreSQL returns for those rows, and write a value it reads back.
 */
class ExtensionContainerSpecConverterTest {

    // The form PostgreSQL returns for a JSONB value: keys sorted by length, a space after ':' and ','.
    private static final String STORED_CONTAINER_SPEC = "{\"env\": {\"GREETING\": \"hello\"}, \"tag\": \"1.4.0\", \"port\": 8090, "
            + "\"image\": \"registry.example.com/acme/billing-ext\", \"replicas\": 2, \"resources\": {\"size\": \"MEDIUM\"}, "
            + "\"registryAuth\": {\"password\": \"${secret:registry;type:TEXT}\", \"registry\": \"registry.example.com\", \"username\": \"acme\"}}";

    private final ExtensionContainerSpecConverter converter = new ExtensionContainerSpecConverter();

    @Test
    void readsAStoredContainerSpec() {
        assertThat(converter.convertToEntityAttribute(STORED_CONTAINER_SPEC)).isEqualTo(containerSpec());
    }

    @Test
    void readsBackWhatItWrites() {
        String json = converter.convertToDatabaseColumn(containerSpec());

        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(containerSpec());
    }

    @Test
    void keepsAnEmptyColumnEmpty() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    private static ExtensionContainerSpec containerSpec() {
        RegistryAuth registryAuth = new RegistryAuth();
        registryAuth.setRegistry("registry.example.com");
        registryAuth.setUsername("acme");
        registryAuth.setPassword("${secret:registry;type:TEXT}");

        ResourceSpec resources = new ResourceSpec();
        resources.setSize(ResourceSize.MEDIUM);

        ExtensionContainerSpec containerSpec = new ExtensionContainerSpec();
        containerSpec.setImage("registry.example.com/acme/billing-ext");
        containerSpec.setTag("1.4.0");
        containerSpec.setRegistryAuth(registryAuth);
        containerSpec.setReplicas(2);
        containerSpec.setPort(8090);
        containerSpec.setEnv(Map.of("GREETING", "hello"));
        containerSpec.setResources(resources);
        return containerSpec;
    }

}
