/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TbEntityLocalSubsInfoTest {

    @Test
    public void addTest() {
        Set<TbAttributeSubscription> expectedSubs = new HashSet<>();
        TbEntityLocalSubsInfo subsInfo = createSubsInfo();
        TenantId tenantId = subsInfo.getTenantId();
        EntityId entityId = subsInfo.getEntityId();
        TbAttributeSubscription attrSubscription1 = TbAttributeSubscription.builder()
                .sessionId("session1")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key1", 1L, "key2", 2L))
                .build();
        expectedSubs.add(attrSubscription1);
        TbEntitySubEvent created = subsInfo.add(attrSubscription1);
        assertFalse(subsInfo.isEmpty());
        assertNotNull(created);
        assertEquals(expectedSubs, subsInfo.getSubs());
        checkEvent(created, expectedSubs, ComponentLifecycleEvent.CREATED);

        assertNull(subsInfo.add(attrSubscription1));

        TbAttributeSubscription attrSubscription2 = TbAttributeSubscription.builder()
                .sessionId("session2")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key3", 3L, "key4", 4L))
                .build();
        expectedSubs.add(attrSubscription2);
        TbEntitySubEvent updated = subsInfo.add(attrSubscription2);
        assertNotNull(updated);

        assertEquals(expectedSubs, subsInfo.getSubs());
        checkEvent(updated, expectedSubs, ComponentLifecycleEvent.UPDATED);
    }

    @Test
    public void removeTest() {
        Set<TbAttributeSubscription> expectedSubs = new HashSet<>();
        TbEntityLocalSubsInfo subsInfo = createSubsInfo();
        TenantId tenantId = subsInfo.getTenantId();
        EntityId entityId = subsInfo.getEntityId();
        TbAttributeSubscription attrSubscription1 = TbAttributeSubscription.builder()
                .sessionId("session1")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key1", 1L, "key2", 2L))
                .build();

        TbAttributeSubscription attrSubscription2 = TbAttributeSubscription.builder()
                .sessionId("session2")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key3", 3L, "key4", 4L))
                .build();

        expectedSubs.add(attrSubscription1);
        expectedSubs.add(attrSubscription2);

        subsInfo.add(attrSubscription1);
        subsInfo.add(attrSubscription2);

        assertEquals(expectedSubs, subsInfo.getSubs());

        TbEntitySubEvent updatedEvent = subsInfo.remove(attrSubscription1);
        expectedSubs.remove(attrSubscription1);
        assertNotNull(updatedEvent);
        assertEquals(expectedSubs, subsInfo.getSubs());
        checkEvent(updatedEvent, expectedSubs, ComponentLifecycleEvent.UPDATED);

        TbEntitySubEvent deletedEvent = subsInfo.remove(attrSubscription2);
        expectedSubs.remove(attrSubscription2);
        assertNotNull(deletedEvent);
        assertEquals(expectedSubs, subsInfo.getSubs());
        checkEvent(deletedEvent, expectedSubs, ComponentLifecycleEvent.DELETED);

        assertTrue(subsInfo.isEmpty());
    }

    @Test
    public void removeAllTest() {
        TbEntityLocalSubsInfo subsInfo = createSubsInfo();
        TenantId tenantId = subsInfo.getTenantId();
        EntityId entityId = subsInfo.getEntityId();
        TbAttributeSubscription attrSubscription1 = TbAttributeSubscription.builder()
                .sessionId("session1")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key1", 1L, "key2", 2L))
                .build();

        TbAttributeSubscription attrSubscription2 = TbAttributeSubscription.builder()
                .sessionId("session2")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key3", 3L, "key4", 4L))
                .build();

        TbAttributeSubscription attrSubscription3 = TbAttributeSubscription.builder()
                .sessionId("session3")
                .tenantId(tenantId)
                .entityId(entityId)
                .keyStates(Map.of("key5", 5L, "key6", 6L))
                .build();

        subsInfo.add(attrSubscription1);
        subsInfo.add(attrSubscription2);
        subsInfo.add(attrSubscription3);

        assertFalse(subsInfo.isEmpty());

        TbEntitySubEvent updatedEvent = subsInfo.removeAll(List.of(attrSubscription1, attrSubscription2));
        assertNotNull(updatedEvent);
        checkEvent(updatedEvent, Set.of(attrSubscription3), ComponentLifecycleEvent.UPDATED);

        assertFalse(subsInfo.isEmpty());

        TbEntitySubEvent deletedEvent = subsInfo.removeAll(List.of(attrSubscription3));
        assertNotNull(deletedEvent);
        checkEvent(deletedEvent, null, ComponentLifecycleEvent.DELETED);

        assertTrue(subsInfo.isEmpty());
    }

    private TbEntityLocalSubsInfo createSubsInfo() {
        return new TbEntityLocalSubsInfo(new TenantId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()));
    }

    private void checkEvent(TbEntitySubEvent event, Set<TbAttributeSubscription> expectedSubs, ComponentLifecycleEvent expectedType) {
        assertEquals(expectedType, event.getType());
        TbSubscriptionsInfo info = event.getInfo();
        if (event.getType() == ComponentLifecycleEvent.DELETED) {
            assertNull(info);
            return;
        }
        assertNotNull(info);
        assertFalse(info.notifications);
        assertFalse(info.alarms);
        assertFalse(info.attrAllKeys);
        assertFalse(info.tsAllKeys);
        assertNull(info.tsKeys);
        assertEquals(getAttrKeys(expectedSubs), info.attrKeys);
    }

    private Set<String> getAttrKeys(Set<TbAttributeSubscription> attributeSubscriptions) {
        return attributeSubscriptions.stream().map(s -> s.getKeyStates().keySet()).flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
