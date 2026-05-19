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
package org.thingsboard.server.service.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.subscription.TbEntityDataSubscriptionService;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.ws.notification.NotificationCommandsHandler;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class DefaultWebSocketServiceTest {

    DefaultWebSocketService service;
    TbTenantProfileCache tenantProfileCache;
    WebSocketMsgEndpoint msgEndpoint;

    @BeforeEach
    void setUp() {
        tenantProfileCache = mock(TbTenantProfileCache.class);
        msgEndpoint = mock(WebSocketMsgEndpoint.class);

        service = new DefaultWebSocketService(
                mock(TbLocalSubscriptionService.class),
                mock(TbEntityDataSubscriptionService.class),
                mock(NotificationCommandsHandler.class),
                msgEndpoint,
                mock(AccessValidator.class),
                mock(AttributesService.class),
                mock(TimeseriesService.class),
                mock(TbServiceInfoProvider.class),
                tenantProfileCache
        );
    }

    // Regression test: publicUserSubscriptionsMap must be keyed by TenantId, not UserId(NULL_UUID).
    // With the old UserId(NULL_UUID) key, all tenants shared one global subscription counter.
    @Test
    void processSubscription_publicUserSubscriptionsMap_isPerTenantNotGlobal() throws Exception {
        int maxPublicSubscriptions = 2;

        TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile1 = new TenantProfile();
        profile1.createDefaultTenantProfileData();
        profile1.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile1).given(tenantProfileCache).get(tenant1);

        TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile2 = new TenantProfile();
        profile2.createDefaultTenantProfileData();
        profile2.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile2).given(tenantProfileCache).get(tenant2);

        // tenant1 fills up its quota
        for (int i = 0; i < maxPublicSubscriptions; i++) {
            assertThat(service.processSubscription(mockPublicSessionRef(tenant1, "t1-session-" + i), subscriptionCmd(i)))
                    .as("tenant1 subscription %d should be accepted", i + 1)
                    .isTrue();
        }

        // tenant2 must have its own independent quota — this was the bug:
        // with UserId(NULL_UUID) as key all tenants shared one counter, so tenant2 would be blocked here
        for (int i = 0; i < maxPublicSubscriptions; i++) {
            assertThat(service.processSubscription(mockPublicSessionRef(tenant2, "t2-session-" + i), subscriptionCmd(i)))
                    .as("tenant2 subscription %d should not be affected by tenant1's subscriptions", i + 1)
                    .isTrue();
        }

        // tenant1's (maxPublicSubscriptions + 1)-th subscription must be rejected
        assertThat(service.processSubscription(mockPublicSessionRef(tenant1, "t1-session-over"), subscriptionCmd(99)))
                .as("tenant1 should be rejected after exceeding its limit")
                .isFalse();

        // Verify that publicUserSubscriptionsMap has separate entries per tenant
        @SuppressWarnings("unchecked")
        ConcurrentMap<TenantId, Set<String>> publicUserSubscriptionsMap =
                (ConcurrentMap<TenantId, Set<String>>) ReflectionTestUtils.getField(service, "publicUserSubscriptionsMap");

        assertThat(publicUserSubscriptionsMap).as("map should contain tenant1").containsKey(tenant1);
        assertThat(publicUserSubscriptionsMap).as("map should contain tenant2").containsKey(tenant2);
        assertThat(publicUserSubscriptionsMap).as("map must not have a single NULL_UUID entry for all tenants")
                .doesNotContainKey(new TenantId(EntityId.NULL_UUID));

        assertThat(publicUserSubscriptionsMap.get(tenant1))
                .as("tenant1 should have exactly %d subscriptions", maxPublicSubscriptions)
                .hasSize(maxPublicSubscriptions);
        assertThat(publicUserSubscriptionsMap.get(tenant2))
                .as("tenant2 should have exactly %d subscriptions", maxPublicSubscriptions)
                .hasSize(maxPublicSubscriptions);
    }

    @Test
    void processSubscription_publicUserSubscriptionsMap_subscriptionIdFormat() {
        int maxPublicSubscriptions = 5;
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile = new TenantProfile();
        profile.createDefaultTenantProfileData();
        profile.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile).given(tenantProfileCache).get(tenantId);

        String sessionId = "my-session-id";
        int cmdId = 42;
        WebSocketSessionRef sessionRef = mockPublicSessionRef(tenantId, sessionId);
        service.processSubscription(sessionRef, subscriptionCmd(cmdId));

        @SuppressWarnings("unchecked")
        ConcurrentMap<TenantId, Set<String>> publicUserSubscriptionsMap =
                (ConcurrentMap<TenantId, Set<String>>) ReflectionTestUtils.getField(service, "publicUserSubscriptionsMap");

        Set<String> subs = publicUserSubscriptionsMap.get(tenantId);
        assertThat(subs).hasSize(1);
        assertThat(subs.iterator().next()).isEqualTo("[" + sessionId + "]:[" + cmdId + "]");
    }

    @Test
    void processSubscription_unsubscribe_removesEntryFromPublicUserSubscriptionsMap() {
        int maxPublicSubscriptions = 5;
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile = new TenantProfile();
        profile.createDefaultTenantProfileData();
        profile.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile).given(tenantProfileCache).get(tenantId);

        String sessionId = "session-1";
        int cmdId = 1;
        WebSocketSessionRef sessionRef = mockPublicSessionRef(tenantId, sessionId);

        service.processSubscription(sessionRef, subscriptionCmd(cmdId));

        @SuppressWarnings("unchecked")
        ConcurrentMap<TenantId, Set<String>> publicUserSubscriptionsMap =
                (ConcurrentMap<TenantId, Set<String>>) ReflectionTestUtils.getField(service, "publicUserSubscriptionsMap");
        assertThat(publicUserSubscriptionsMap.get(tenantId)).hasSize(1);

        AttributesSubscriptionCmd unsubCmd = subscriptionCmd(cmdId);
        unsubCmd.setUnsubscribe(true);
        service.processSubscription(sessionRef, unsubCmd);

        assertThat(publicUserSubscriptionsMap.get(tenantId)).isEmpty();
    }

    @Test
    void processSubscription_unsubscribe_freesSlotForNewSubscription() {
        int maxPublicSubscriptions = 1;
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile = new TenantProfile();
        profile.createDefaultTenantProfileData();
        profile.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile).given(tenantProfileCache).get(tenantId);

        WebSocketSessionRef sessionRef = mockPublicSessionRef(tenantId, "session-1");
        service.processSubscription(sessionRef, subscriptionCmd(1));

        // slot is full — second subscription on same session should be rejected
        assertThat(service.processSubscription(sessionRef, subscriptionCmd(2))).isFalse();

        // unsubscribe cmd 1 to free the slot
        AttributesSubscriptionCmd unsubCmd = subscriptionCmd(1);
        unsubCmd.setUnsubscribe(true);
        service.processSubscription(sessionRef, unsubCmd);

        // now a new subscription should succeed
        assertThat(service.processSubscription(sessionRef, subscriptionCmd(3)))
                .as("new subscription should succeed after unsubscribe freed the slot")
                .isTrue();
    }

    @Test
    void processSessionClose_removesAllSessionSubscriptionsFromPublicUserSubscriptionsMap() {
        int maxPublicSubscriptions = 10;
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile = new TenantProfile();
        profile.createDefaultTenantProfileData();
        profile.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile).given(tenantProfileCache).get(tenantId);

        String sessionId = "closing-session";
        WebSocketSessionRef sessionRef = mockPublicSessionRef(tenantId, sessionId);

        service.processSubscription(sessionRef, subscriptionCmd(1));
        service.processSubscription(sessionRef, subscriptionCmd(2));
        service.processSubscription(sessionRef, subscriptionCmd(3));

        @SuppressWarnings("unchecked")
        ConcurrentMap<TenantId, Set<String>> publicUserSubscriptionsMap =
                (ConcurrentMap<TenantId, Set<String>>) ReflectionTestUtils.getField(service, "publicUserSubscriptionsMap");
        assertThat(publicUserSubscriptionsMap.get(tenantId)).hasSize(3);

        service.processSessionClose(sessionRef);

        assertThat(publicUserSubscriptionsMap.get(tenantId)).isEmpty();
    }

    @Test
    void processSessionClose_onlyRemovesClosedSessionSubscriptions() {
        int maxPublicSubscriptions = 10;
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile = new TenantProfile();
        profile.createDefaultTenantProfileData();
        profile.getDefaultProfileConfiguration().setMaxWsSubscriptionsPerPublicUser(maxPublicSubscriptions);
        willReturn(profile).given(tenantProfileCache).get(tenantId);

        WebSocketSessionRef session1 = mockPublicSessionRef(tenantId, "session-1");
        WebSocketSessionRef session2 = mockPublicSessionRef(tenantId, "session-2");

        service.processSubscription(session1, subscriptionCmd(1));
        service.processSubscription(session1, subscriptionCmd(2));
        service.processSubscription(session2, subscriptionCmd(1));

        @SuppressWarnings("unchecked")
        ConcurrentMap<TenantId, Set<String>> publicUserSubscriptionsMap =
                (ConcurrentMap<TenantId, Set<String>>) ReflectionTestUtils.getField(service, "publicUserSubscriptionsMap");
        assertThat(publicUserSubscriptionsMap.get(tenantId)).hasSize(3);

        service.processSessionClose(session1);

        Set<String> remaining = publicUserSubscriptionsMap.get(tenantId);
        assertThat(remaining).hasSize(1);
        assertThat(remaining).allMatch(subId -> subId.startsWith("[session-2]"));
    }

    private WebSocketSessionRef mockPublicSessionRef(TenantId tenantId, String sessionId) {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        SecurityUser securityUser = mock(SecurityUser.class);
        willReturn(tenantId).given(securityUser).getTenantId();
        willReturn(customerId).given(securityUser).getCustomerId();
        willReturn(new UserId(EntityId.NULL_UUID)).given(securityUser).getId();
        willReturn(true).given(securityUser).isCustomerUser();
        willReturn(new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, customerId.toString())).given(securityUser).getUserPrincipal();

        WebSocketSessionRef ref = mock(WebSocketSessionRef.class);
        willReturn(securityUser).given(ref).getSecurityCtx();
        willReturn(sessionId).given(ref).getSessionId();
        return ref;
    }

    private AttributesSubscriptionCmd subscriptionCmd(int cmdId) {
        AttributesSubscriptionCmd cmd = new AttributesSubscriptionCmd();
        cmd.setCmdId(cmdId);
        return cmd;
    }

}
