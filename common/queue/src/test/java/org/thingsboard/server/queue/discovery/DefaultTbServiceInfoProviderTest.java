// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTbServiceInfoProviderTest {

    private DefaultTbServiceInfoProvider provider;

    @BeforeEach
    void setUp() {
        TbTransportService snmp = mock(TbTransportService.class);
        when(snmp.getName()).thenReturn(DataConstants.SNMP_TRANSPORT_NAME);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(TbTransportService.class)).thenReturn(Map.of("snmpTransportService", snmp));

        provider = new DefaultTbServiceInfoProvider();
        ReflectionTestUtils.setField(provider, "serviceId", "tb-transport-1");
        ReflectionTestUtils.setField(provider, "serviceType", "tb-transport");
        ReflectionTestUtils.setField(provider, "supportedIntegrationsStr", "ALL");
        ReflectionTestUtils.setField(provider, "excludedIntegrationsStr", "NONE");
        ReflectionTestUtils.setField(provider, "applicationContext", applicationContext);
        provider.init();
    }

    @Test
    void transportsAreKnownAfterContextIsReady() {
        assertThat(provider.getServiceInfo().getTransportsList()).isEmpty();

        provider.setTransports();

        assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
    }

    @Test
    void transportsSurviveThePeriodicRegeneration() {
        provider.setTransports();

        // what the discovery service does every minute before re-publishing this node
        assertThat(provider.generateNewServiceInfoWithCurrentSystemInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
        assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
    }

    @Test
    void transportsSurviveTheRegenerationOnReadyFlagChange() {
        provider.setTransports();

        // without ZooKeeper the ready flag is the only thing that regenerates the info
        new DummyDiscoveryService(provider, mock(PartitionService.class)).setReady(false);

        assertThat(provider.getServiceInfo().getReady()).isFalse();
        assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
    }

    @Test
    void transportsAreNotDuplicatedWhenSetAgain() {
        provider.setTransports();
        provider.setTransports();

        assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
    }

}
