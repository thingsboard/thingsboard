/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.auth;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class SessionInfoCreator {

    public static TransportProtos.SessionInfoProto create(ValidateDeviceCredentialsResponse msg, TransportContext context, UUID sessionId) {
        TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder();
        builder.setNodeId(context.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits());
        if (!"null".equals(msg.getDeviceInfo().getAdditionalInfo())) {
            try {
                JsonNode infoNode = context.getMapper().readTree(msg.getDeviceInfo().getAdditionalInfo());
                if (infoNode.get("gateway").asBoolean()) {
                    builder.setActivityTimeFromGatewayDevice(infoNode.get("activityTimeFromGatewayDevice").asBoolean());
                }
            } catch (IOException e) {
                log.trace("[{}][{}] Failed to fetch device additional info", sessionId, msg.getDeviceInfo().getDeviceName(), e);
            }
        }
        return builder.build();
    }

}
