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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@TbCoreComponent
public class AdminSettingsEdgeProcessor extends BaseEdgeProcessor {

    private static final Set<String> EDGE_SYNCED_SETTINGS_KEYS = Set.of("general", "connectivity");

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AdminSettings adminSettings = null;
        if (edgeEvent.getEntityId() != null) {
            AdminSettingsId adminSettingsId = new AdminSettingsId(edgeEvent.getEntityId());
            adminSettings = edgeCtx.getAdminSettingsService().findAdminSettingsById(edgeEvent.getTenantId(), adminSettingsId);
        } else if (edgeEvent.getBody() != null && !edgeEvent.getBody().isEmpty()) {
            // legacy
            adminSettings = JacksonUtil.convertValue(edgeEvent.getBody(), AdminSettings.class);
        }
        if (adminSettings == null) {
            return null;
        }
        if (TenantId.SYS_TENANT_ID.equals(adminSettings.getTenantId())
                || !EDGE_SYNCED_SETTINGS_KEYS.contains(adminSettings.getKey())) {
            log.trace("Skipping admin settings [{}] sync to edge [{}] - not in the edge allow-list",
                    adminSettings.getKey(), edgeEvent.getEdgeId());
            return null;
        }
        AdminSettingsUpdateMsg msg = AdminSettingsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(adminSettings)).build();
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(msg)
                .build();
    }

    public List<DownlinkMsg> convertAdminSettingsCleanupToDownlinks(Edge edge) {
        List<DownlinkMsg> result = new ArrayList<>();
        Set<String> cleanedKeys = new HashSet<>();

        Set<TenantId> tenantScopes = Set.of(TenantId.SYS_TENANT_ID, edge.getTenantId());
        for (TenantId scope : tenantScopes) {
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<AdminSettings> page;
            do {
                page = edgeCtx.getAdminSettingsService().findAllByTenantId(scope, pageLink);
                for (AdminSettings adminSettings : page.getData()) {
                    String key = adminSettings.getKey();
                    if (EDGE_SYNCED_SETTINGS_KEYS.contains(key) || !cleanedKeys.add(key)) {
                        continue;
                    }
                    result.add(buildAdminSettingsCleanupDownlink(key));
                }
                pageLink = pageLink.nextPageLink();
            } while (page.hasNext());
        }
        log.info("[{}][{}] Prepared {} one-time admin settings cleanup message(s): {}",
                edge.getTenantId(), edge.getId(), result.size(), cleanedKeys);
        return result;
    }

    private DownlinkMsg buildAdminSettingsCleanupDownlink(String key) {
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminSettings.setKey(key);
        adminSettings.setJsonValue(buildAdminSettingsCleanupValue(key));
        AdminSettingsUpdateMsg msg = AdminSettingsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(adminSettings)).build();
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(msg)
                .build();
    }

    private JsonNode buildAdminSettingsCleanupValue(String key) {
        if (JwtSettingsService.ADMIN_SETTINGS_JWT_KEY.equals(key)) {
            return JacksonUtil.valueToTree(generateRandomJwtSettings());
        }
        ObjectNode value = JacksonUtil.newObjectNode();
        if ("mail".equals(key)) {
            value.put("password", "");
            value.put("refreshToken", "");
        }
        return value;
    }

    private JwtSettings generateRandomJwtSettings() {
        String randomSigningKey = Base64.getEncoder().encodeToString(
                RandomStringUtils.secure().nextAlphanumeric(64).getBytes(StandardCharsets.UTF_8));
        return new JwtSettings(tokenExpirationTime, refreshTokenExpTime, tokenIssuer, randomSigningKey);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ADMIN_SETTINGS;
    }

}
