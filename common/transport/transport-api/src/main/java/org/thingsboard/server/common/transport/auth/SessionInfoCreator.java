// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

@Slf4j
public class SessionInfoCreator {

    public static TransportProtos.SessionInfoProto create(ValidateDeviceCredentialsResponse msg, TransportContext context, UUID sessionId) {
        return getSessionInfoProto(msg, context.getNodeId(), sessionId);
    }

    public static TransportProtos.SessionInfoProto create(ValidateDeviceCredentialsResponse msg, String nodeId, UUID sessionId) {
        return getSessionInfoProto(msg, nodeId, sessionId);
    }

    private static TransportProtos.SessionInfoProto getSessionInfoProto(ValidateDeviceCredentialsResponse msg, String nodeId, UUID sessionId) {
        return TransportProtos.SessionInfoProto.newBuilder().setNodeId(nodeId)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .setIsGateway(msg.getDeviceInfo().isGateway())
                .build();
    }

}
