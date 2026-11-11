// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edqs;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.edqs.processor.EdqsProducer;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.edqs.util.EdqsMapper;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.environment.DistributedLockService;
import org.thingsboard.server.queue.provider.EdqsClientQueueFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the producer half of the delete-tombstone protocol: {@link DefaultEdqsService#processEvent} is the only
 * place that stamps {@code versionsResetOnDelete} (bound from {@code database.citus.enabled}) onto outgoing
 * {@link org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg}s, and it must do so exactly for
 * {@code DELETED} events when the flag is enabled — the consumer-side gate
 * ({@code EdqsProcessorTombstoneTest}) is pinned separately over hand-built messages.
 */
class DefaultEdqsServiceTest {

    private static final String OBJECT_KEY = "objectKey";
    private static final long OBJECT_VERSION = 10L;

    private DefaultEdqsService service;
    private EdqsProducer eventsProducer;
    private CitusSettings citusSettings;
    private EdqsObject object;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        EdqsMapper edqsMapper = mock(EdqsMapper.class);
        citusSettings = mock(CitusSettings.class);
        service = new DefaultEdqsService(
                mock(EdqsClientQueueFactory.class), edqsMapper, mock(EdqsSyncService.class),
                mock(EdqsApiService.class), mock(DistributedLockService.class), mock(AttributesService.class),
                mock(EdqsPartitionService.class), mock(TbServiceInfoProvider.class), mock(DiscoveryService.class),
                citusSettings);
        // processEvent submits to the executor and sends via the producer normally built in init(); a direct
        // executor and a mocked producer are injected instead so the stamped message can be captured synchronously.
        ReflectionTestUtils.setField(service, "executor", MoreExecutors.newDirectExecutorService());
        eventsProducer = mock(EdqsProducer.class);
        ReflectionTestUtils.setField(service, "eventsProducer", eventsProducer);

        object = mock(EdqsObject.class);
        when(object.stringKey()).thenReturn(OBJECT_KEY);
        when(object.version()).thenReturn(OBJECT_VERSION);
        when(edqsMapper.serialize(any())).thenReturn(new byte[0]);
    }

    @ParameterizedTest(name = "versionsResetOnDelete={0}, eventType={1} -> flag stamped={2}")
    @CsvSource({
            "true, DELETED, true",
            "true, UPDATED, false",
            "false, DELETED, false",
            "false, UPDATED, false"
    })
    void processEventStampsVersionsResetOnDeleteOnlyForDeletesWhenEnabled(boolean versionsResetOnDelete,
                                                                          EdqsEventType eventType,
                                                                          boolean expectedFlag) {
        when(citusSettings.isEnabled()).thenReturn(versionsResetOnDelete);

        service.processEvent(tenantId, ObjectType.DEVICE, eventType, object);

        ArgumentCaptor<ToEdqsMsg> msgCaptor = ArgumentCaptor.forClass(ToEdqsMsg.class);
        verify(eventsProducer).send(eq(tenantId), eq(ObjectType.DEVICE), eq(OBJECT_KEY), msgCaptor.capture());
        ToEdqsMsg msg = msgCaptor.getValue();
        assertThat(msg.getEventMsg().getEventType()).isEqualTo(eventType.name());
        assertThat(msg.getEventMsg().getVersion()).isEqualTo(OBJECT_VERSION);
        assertThat(msg.getEventMsg().getVersionsResetOnDelete()).isEqualTo(expectedFlag);
    }

}
