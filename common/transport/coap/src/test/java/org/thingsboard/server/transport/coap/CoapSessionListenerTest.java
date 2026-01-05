package org.thingsboard.server.transport.coap;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.coapserver.CoapServerContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.transport.coap.client.DefaultCoapClientContext;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;
import org.thingsboard.server.transport.coap.client.TbCoapObservationState;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CoapSessionListenerTest {

    @Mock
    CoapServerContext config;
    @Mock CoapTransportContext transportContext;
    @Mock
    TransportService transportService;
    @Mock
    TransportDeviceProfileCache profileCache;
    @Mock
    PartitionService partitionService;

    private DefaultCoapClientContext coapClientContext;

    @BeforeEach
    void setUp() {
        coapClientContext = new DefaultCoapClientContext(config, transportContext, transportService, profileCache, partitionService);
    }

    @Test
    void onTenantDeleted_shouldNotInteractWithCore() {
        // Arrange
        TbCoapClientState state = mock(TbCoapClientState.class);

        TbCoapObservationState rpcObs = mock(TbCoapObservationState.class);
        when(rpcObs.getToken()).thenReturn("rpc-token");
        when(rpcObs.getExchange()).thenReturn(mock(CoapExchange.class));

        TbCoapObservationState attrsObs = mock(TbCoapObservationState.class);
        when(attrsObs.getToken()).thenReturn("attr-token");
        when(attrsObs.getExchange()).thenReturn(mock(CoapExchange.class));
        when(state.getRpc()).thenReturn(rpcObs);
        when(state.getAttrs()).thenReturn(attrsObs);

        DefaultCoapClientContext.CoapSessionListener listener = coapClientContext.new CoapSessionListener(state);

        // Act
        listener.onTenantDeleted(mock(DeviceId.class));

        // Assert
        verify(transportService, never())
                .process(
                        any(TransportProtos.SessionInfoProto.class),
                        any(TransportProtos.SessionEventMsg.class),
                        any()
                );
    }

    @Test
    void onTenantDeleted_shouldDeregisterSession() {
        // GIVEN
        TbCoapClientState state = mock(TbCoapClientState.class);

        TbCoapObservationState rpcObs = mock(TbCoapObservationState.class);
        when(rpcObs.getToken()).thenReturn("rpc-token");
        when(rpcObs.getExchange()).thenReturn(mock(CoapExchange.class));

        when(state.getRpc()).thenReturn(rpcObs);
        when(state.getAttrs()).thenReturn(null);

        DefaultCoapClientContext.CoapSessionListener listener = coapClientContext.new CoapSessionListener(state);

        // WHEN
        listener.onTenantDeleted(mock(DeviceId.class));

        // THEN
        verify(transportService, never())
                .process(
                        any(TransportProtos.SessionInfoProto.class),
                        any(TransportProtos.SessionEventMsg.class),
                        any()
                );
        verify(transportService, times(1))
                .deregisterSession(any());
    }

}
