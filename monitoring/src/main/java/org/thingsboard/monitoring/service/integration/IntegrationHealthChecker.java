// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.service.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.integration.IntegrationInfo;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringConfig;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.config.integration.IntegrationType;
import org.thingsboard.monitoring.service.BaseHealthChecker;

@Slf4j
public abstract class IntegrationHealthChecker<C extends IntegrationMonitoringConfig> extends BaseHealthChecker<C, IntegrationMonitoringTarget> {

    public IntegrationHealthChecker(C config, IntegrationMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected final void initialize() {
        entityService.checkEntities(config, target);
    }

    @Override
    protected final String createTestPayload(String testValue) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        payload.set("telemetry", JacksonUtil.newObjectNode()
                .set(TEST_TELEMETRY_KEY, new TextNode(testValue)));
        payload.set("device", new TextNode(target.getDevice().getName()));
        return payload.toString();
    }

    @Override
    protected final Object getInfo() {
        return new IntegrationInfo(getIntegrationType(), target.getBaseUrl());
    }

    @Override
    protected final String getKey() {
        return getIntegrationType().name().toLowerCase() + "Integration";
    }

    protected abstract IntegrationType getIntegrationType();

    @Override
    protected boolean isCfMonitoringEnabled() {
        return false;
    }

}
