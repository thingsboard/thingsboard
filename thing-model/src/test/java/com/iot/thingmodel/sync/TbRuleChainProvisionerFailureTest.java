package com.iot.thingmodel.sync;

import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.support.TbClientStubs;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TbRuleChainProvisionerFailureTest extends TbRuleChainProvisionerTestSupport {

    @Test
    void failsWhenCreateReturnsNull() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        TbClientStubs.stubNoRuleChains(client);
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(null);

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> new TbRuleChainProvisioner(provider, properties(), objectMapper)
                        .ensure(request(tenantId)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getHttpCode());
        verify(client, never()).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void failsWhenCreatedChainHasNoId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        TbClientStubs.stubNoRuleChains(client);
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(new RuleChain());

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> new TbRuleChainProvisioner(provider, properties(), objectMapper)
                        .ensure(request(tenantId)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getHttpCode());
        verify(client, never()).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingModelProperties properties = properties();
        properties.getRuleChain().setEnabled(false);

        UUID actual = new TbRuleChainProvisioner(provider, properties, objectMapper)
                .ensure(request(tenantId));

        assertNull(actual);
        verifyNoInteractions(provider);
    }
}
