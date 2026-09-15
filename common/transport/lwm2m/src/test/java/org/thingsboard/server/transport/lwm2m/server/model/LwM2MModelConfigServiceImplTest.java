// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class LwM2MModelConfigServiceImplTest {

    LwM2MModelConfigServiceImpl service;
    TbLwM2MModelConfigStore modelStore;

    @BeforeEach
    void setUp() {
        service = new LwM2MModelConfigServiceImpl();
        modelStore = mock(TbLwM2MModelConfigStore.class);
        service.modelStore = modelStore;
    }

    @Test
    void testInitWithDuplicatedModels() {
        LwM2MModelConfig config = new LwM2MModelConfig("urn:imei:951358811362976");
        List<LwM2MModelConfig> models = List.of(config, config);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyEntriesOf(Map.of(config.getEndpoint(), config));
    }

    @Test
    void testInitWithNonUniqueEndpoints() {
        LwM2MModelConfig configAlfa = new LwM2MModelConfig("urn:imei:951358811362976");
        LwM2MModelConfig configBravo = new LwM2MModelConfig("urn:imei:151358811362976");
        LwM2MModelConfig configDelta = new LwM2MModelConfig("urn:imei:151358811362976");
        assertThat(configBravo.getEndpoint()).as("non-unique endpoints provided").isEqualTo(configDelta.getEndpoint());
        List<LwM2MModelConfig> models = List.of(configAlfa, configBravo, configDelta);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                configAlfa.getEndpoint(), configAlfa,
                configBravo.getEndpoint(), configBravo
        ));
    }

    @Test
    void testInitWithEmptyModels() {
        willReturn(Collections.emptyList()).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).isEmpty();
    }

}
