// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.service.transport;

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.BaseHealthChecker;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> extends BaseHealthChecker<C, TransportMonitoringTarget> {

    @Value("${monitoring.calculated_fields.enabled:true}")
    private boolean calculatedFieldsMonitoringEnabled;

    public TransportHealthChecker(C config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initialize() {
        entityService.checkEntities(config, target);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    @Override
    protected Object getInfo() {
        return new TransportInfo(getTransportType(), target);
    }

    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + (target.getQueue().equals("Main") ? "" : target.getQueue()) + "Transport";
    }

    protected abstract TransportType getTransportType();

    @Override
    protected boolean isCfMonitoringEnabled() {
        return calculatedFieldsMonitoringEnabled;
    }

}
