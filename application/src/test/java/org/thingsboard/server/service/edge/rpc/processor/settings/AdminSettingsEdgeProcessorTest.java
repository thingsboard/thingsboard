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
package org.thingsboard.server.service.edge.rpc.processor.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminSettingsEdgeProcessorTest {

    @Mock
    private EdgeContextComponent edgeCtx;
    @Mock
    private AdminSettingsService adminSettingsService;

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private AdminSettingsEdgeProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new AdminSettingsEdgeProcessor();
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        ReflectionTestUtils.setField(processor, "tokenExpirationTime", 9000);
        ReflectionTestUtils.setField(processor, "refreshTokenExpTime", 604800);
        ReflectionTestUtils.setField(processor, "tokenIssuer", "thingsboard.io");
        lenient().when(edgeCtx.getAdminSettingsService()).thenReturn(adminSettingsService);
    }

    @Test
    public void testTenantScopedAllowListedKeyIsSyncedToEdge() {
        AdminSettings general = adminSettings(tenantId, "general");
        when(adminSettingsService.findAdminSettingsById(any(), any())).thenReturn(general);

        DownlinkMsg downlink = processor.convertEdgeEventToDownlink(edgeEvent(tenantId), EdgeVersion.V_4_2_2_4);

        Assertions.assertNotNull(downlink);
        Assertions.assertEquals(1, downlink.getAdminSettingsUpdateMsgCount());
        AdminSettings synced = JacksonUtil.fromString(downlink.getAdminSettingsUpdateMsg(0).getEntity(), AdminSettings.class, true);
        Assertions.assertNotNull(synced);
        Assertions.assertEquals("general", synced.getKey());
    }

    @Test
    public void testTenantScopedNonAllowListedKeyIsNotSyncedToEdge() {
        AdminSettings mail = adminSettings(tenantId, "mail");
        when(adminSettingsService.findAdminSettingsById(any(), any())).thenReturn(mail);

        DownlinkMsg downlink = processor.convertEdgeEventToDownlink(edgeEvent(tenantId), EdgeVersion.V_4_2_2_4);

        Assertions.assertNull(downlink);
    }

    @Test
    public void testSystemScopedAllowListedKeyIsNotSyncedToEdge() {
        AdminSettings general = adminSettings(TenantId.SYS_TENANT_ID, "general");
        when(adminSettingsService.findAdminSettingsById(any(), any())).thenReturn(general);

        DownlinkMsg downlink = processor.convertEdgeEventToDownlink(edgeEvent(TenantId.SYS_TENANT_ID), EdgeVersion.V_4_2_2_4);

        Assertions.assertNull(downlink);
    }

    @Test
    public void testCleanupClearsSecretsAndSkipsAllowListedKeys() {
        Edge edge = new Edge();
        edge.setId(new EdgeId(UUID.randomUUID()));
        edge.setTenantId(tenantId);

        when(adminSettingsService.findAllByTenantId(eq(TenantId.SYS_TENANT_ID), any()))
                .thenReturn(page(adminSettings(TenantId.SYS_TENANT_ID, "mail"),
                        adminSettings(TenantId.SYS_TENANT_ID, "jwt"),
                        adminSettings(TenantId.SYS_TENANT_ID, "general")));
        when(adminSettingsService.findAllByTenantId(eq(tenantId), any()))
                .thenReturn(page(adminSettings(tenantId, "sms"),
                        adminSettings(tenantId, "connectivity")));

        List<DownlinkMsg> downlinks = processor.convertAdminSettingsCleanupToDownlinks(edge);

        Map<String, AdminSettings> cleanupByKey = downlinks.stream()
                .map(d -> JacksonUtil.fromString(d.getAdminSettingsUpdateMsg(0).getEntity(), AdminSettings.class, true))
                .collect(Collectors.toMap(AdminSettings::getKey, s -> s));

        // Allow-listed keys (general/connectivity) are never wiped; every other key is.
        Assertions.assertEquals(Set.of("mail", "jwt", "sms"), cleanupByKey.keySet());

        // Mail secrets are blanked.
        JsonNode mailValue = cleanupByKey.get("mail").getJsonValue();
        Assertions.assertEquals("", mailValue.get("password").asText());
        Assertions.assertEquals("", mailValue.get("refreshToken").asText());

        // JWT signing key is re-randomized to a non-empty value.
        JsonNode jwtValue = cleanupByKey.get("jwt").getJsonValue();
        Assertions.assertFalse(jwtValue.get("tokenSigningKey").asText().isEmpty());

        // Other non-allow-listed keys are wiped to an empty object.
        Assertions.assertTrue(cleanupByKey.get("sms").getJsonValue().isEmpty());
    }

    private EdgeEvent edgeEvent(TenantId eventTenantId) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(eventTenantId);
        edgeEvent.setEdgeId(new EdgeId(UUID.randomUUID()));
        edgeEvent.setEntityId(UUID.randomUUID());
        return edgeEvent;
    }

    private AdminSettings adminSettings(TenantId settingsTenantId, String key) {
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setId(new AdminSettingsId(UUID.randomUUID()));
        adminSettings.setTenantId(settingsTenantId);
        adminSettings.setKey(key);
        adminSettings.setJsonValue(JacksonUtil.newObjectNode());
        return adminSettings;
    }

    private PageData<AdminSettings> page(AdminSettings... settings) {
        List<AdminSettings> data = List.of(settings);
        return new PageData<>(data, 1, data.size(), false);
    }

}
