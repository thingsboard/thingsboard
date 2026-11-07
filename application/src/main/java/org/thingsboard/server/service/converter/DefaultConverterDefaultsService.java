// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@TbCoreComponent
@Service
@Slf4j
public class DefaultConverterDefaultsService implements ConverterDefaultsService {

    private static final String CONVERTERS_DIR = "converters";
    private static final String PAYLOAD_FILE = "payload.json";
    private static final String METADATA_FILE = "metadata.json";
    private static final String INTEGRATION_FILE = "integration.json";

    @Override
    public Optional<String> getPayload(IntegrationType integrationType, ConverterType converterType) {
        return getFileContent(integrationType, converterType.getDirectory() + "/" + PAYLOAD_FILE);
    }

    @Override
    public Optional<String> getMetadata(IntegrationType integrationType, ConverterType converterType) {
        return getFileContent(integrationType, converterType.getDirectory() + "/" + METADATA_FILE);
    }

    @Override
    public Optional<String> getIntegrationName(IntegrationType integrationType) {
        return getFileContent(integrationType, INTEGRATION_FILE)
                .map(content -> JacksonUtil.toJsonNode(content).get("name").asText());
    }

    private Optional<String> getFileContent(IntegrationType integrationType, String relativePath) {
        if (integrationType.getDirectory() == null) {
            return Optional.empty();
        }
        String path = String.join("/", CONVERTERS_DIR, integrationType.getDirectory(), relativePath);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Failed to read converter defaults file {}", path, e);
            return Optional.empty();
        }
    }

}
