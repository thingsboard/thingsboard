// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.integration.RemoteIntegrationRpcService;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-rule-engine' && '${integrations.rpc.enabled:false}'=='true'")
public class TbRuleEngineRemoteIntegrationRpcService implements RemoteIntegrationRpcService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final RemoteIntegrationSessionService sessionsCache;
    private final TbClusterService clusterService;

    @Override
    public void updateIntegration(Integration configuration) {
    }

    @Override
    public void updateConverter(Converter converter) {
    }

    @Override
    public boolean handleRemoteDownlink(IntegrationDownlinkMsg msg) {
        IntegrationSession remoteSession = sessionsCache.findIntegrationSession(msg.getIntegrationId());
        if (remoteSession != null && !remoteSession.getServiceId().equals(serviceInfoProvider.getServiceId())) {
            log.debug("[{}] Remote integration session found for [{}] downlink @ Server [{}].", msg.getIntegrationId(), msg.getEntityId(), remoteSession.getServiceId());
            clusterService.pushNotificationToCore(remoteSession.getServiceId(), msg, null);
            return true;
        }
        return false;
    }

}
