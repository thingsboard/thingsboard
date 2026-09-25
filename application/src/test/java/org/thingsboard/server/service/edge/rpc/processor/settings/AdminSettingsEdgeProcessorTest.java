// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

}
