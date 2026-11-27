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

    private static final String SNMP = DataConstants.SNMP_TRANSPORT_NAME;
    private static final String LWM2M = DataConstants.LWM2M_TRANSPORT_NAME;

    private DefaultTbServiceInfoProvider provider;

    @BeforeEach
    void setUp() {
        TbTransportService snmp = mock(TbTransportService.class);
        when(snmp.getName()).thenReturn(SNMP);
        TbTransportService lwm2m = mock(TbTransportService.class);
        when(lwm2m.getName()).thenReturn(LWM2M);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(TbTransportService.class))
                .thenReturn(Map.of("snmpTransportService", snmp, "lwM2MTransportService", lwm2m));

        provider = new DefaultTbServiceInfoProvider();
        ReflectionTestUtils.setField(provider, "serviceId", "tb-transport-1");
        ReflectionTestUtils.setField(provider, "serviceType", "tb-transport");
        ReflectionTestUtils.setField(provider, "applicationContext", applicationContext);
        provider.init();
    }

    @Test
    void transportsAreKnownAfterContextIsReady() {
        assertThat(provider.getServiceInfo().getTransportsList()).isEmpty();

        provider.setTransports();

        assertThat(provider.getServiceInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
    }

    @Test
    void transportsSurviveThePeriodicRegeneration() {
        provider.setTransports();

        // what the discovery service does every minute before re-publishing this node
        assertThat(provider.generateNewServiceInfoWithCurrentSystemInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
        assertThat(provider.getServiceInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
    }

    @Test
    void transportsSurviveTheRegenerationOnReadyFlagChange() {
        provider.setTransports();

        // without ZooKeeper the ready flag is the only thing that regenerates the info
        new DummyDiscoveryService(provider, mock(PartitionService.class)).setReady(false);

        assertThat(provider.getServiceInfo().getReady()).isFalse();
        assertThat(provider.getServiceInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
    }

    @Test
    void transportsAreNotDuplicatedWhenSetAgain() {
        provider.setTransports();
        provider.setTransports();

        assertThat(provider.getServiceInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
    }

    @Test
    void transportsAreNotDuplicatedWhenSetAfterRegeneration() {
        provider.setTransports();
        provider.generateNewServiceInfoWithCurrentSystemInfo();

        provider.setTransports();

        assertThat(provider.getServiceInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
        assertThat(provider.generateNewServiceInfoWithCurrentSystemInfo().getTransportsList()).containsExactlyInAnyOrder(SNMP, LWM2M);
    }

}
