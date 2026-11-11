// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.processor;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;
import org.thingsboard.server.edqs.repo.EdqsRepository;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.edqs.state.EdqsStateService;
import org.thingsboard.server.edqs.util.EdqsMapper;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsExecutors;
import org.thingsboard.server.queue.edqs.EdqsQueueFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared harness for the tombstone tests: builds a real {@link EdqsProcessor} over mocked collaborators, injects
 * a fresh {@link VersionsStore} into its private field, stubs the mapper so every event resolves to the subclass's
 * single {@link #objectKey()}, and provides the parameterized {@link #event} builder. Keeping the reflective
 * wiring (the {@code versionsStore} field name) in one place means a rename breaks a single class.
 */
abstract class AbstractEdqsProcessorTombstoneTest {

    protected EdqsProcessor processor;
    protected EdqsRepository repository;
    protected VersionsStore versionsStore;

    @BeforeEach
    void setUpProcessor() {
        EdqsMapper mapper = mock(EdqsMapper.class);
        repository = mock(EdqsRepository.class);
        processor = new EdqsProcessor(
                mock(EdqsQueueFactory.class), mapper, repository, mock(EdqsConfig.class),
                mock(EdqsExecutors.class), mock(EdqsPartitionService.class), mock(DiscoveryService.class),
                mock(TopicService.class), mock(ConfigurableApplicationContext.class), mock(EdqsStateService.class));

        versionsStore = new VersionsStore(60);
        ReflectionTestUtils.setField(processor, "versionsStore", versionsStore);

        EdqsObject object = mock(EdqsObject.class);
        when(mapper.deserialize(any(ObjectType.class), any(), anyBoolean())).thenReturn(object);
        when(mapper.getKey(any())).thenReturn(objectKey());
    }

    @AfterEach
    void tearDownProcessor() {
        versionsStore.shutdown();
    }

    /**
     * The single {@link EdqsObjectKey} every event built by the subclass resolves to.
     */
    protected abstract EdqsObjectKey objectKey();

    protected static ToEdqsMsg event(ObjectType objectType, EdqsEventType eventType, long version, boolean versionsResetOnDelete) {
        EdqsEventMsg eventMsg = EdqsEventMsg.newBuilder()
                .setObjectType(objectType.name())
                .setEventType(eventType.name())
                .setVersion(version)
                .setVersionsResetOnDelete(versionsResetOnDelete)
                .setData(ByteString.EMPTY)
                .build();
        return ToEdqsMsg.newBuilder().setEventMsg(eventMsg).build();
    }

}
