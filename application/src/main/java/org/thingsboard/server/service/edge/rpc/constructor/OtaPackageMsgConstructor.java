/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;

@Component
@TbCoreComponent
public class OtaPackageMsgConstructor {

    public OtaPackageUpdateMsg constructOtaPackageUpdatedMsg(UpdateMsgType msgType, OtaPackage otaPackage) {
        OtaPackageUpdateMsg.Builder builder = OtaPackageUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(otaPackage.getId().getId().getMostSignificantBits())
                .setIdLSB(otaPackage.getId().getId().getLeastSignificantBits())
                .setType(otaPackage.getType().name())
                .setTitle(otaPackage.getTitle())
                .setVersion(otaPackage.getVersion())
                .setTag(otaPackage.getTag());

        if (otaPackage.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(otaPackage.getDeviceProfileId().getId().getMostSignificantBits())
                    .setDeviceProfileIdLSB(otaPackage.getDeviceProfileId().getId().getLeastSignificantBits());
        }

        if (otaPackage.getUrl() != null) {
            builder.setUrl(otaPackage.getUrl());
        }
        if (otaPackage.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(otaPackage.getAdditionalInfo()));
        }
        if (otaPackage.getFileName() != null) {
            builder.setFileName(otaPackage.getFileName());
        }
        if (otaPackage.getContentType() != null) {
            builder.setContentType(otaPackage.getContentType());
        }
        if (otaPackage.getChecksumAlgorithm() != null) {
            builder.setChecksumAlgorithm(otaPackage.getChecksumAlgorithm().name());
        }
        if (otaPackage.getChecksum() != null) {
            builder.setChecksum(otaPackage.getChecksum());
        }
        if (otaPackage.getDataSize() != null) {
            builder.setDataSize(otaPackage.getDataSize());
        }
        if (otaPackage.getData() != null) {
            try {
                //TODO: Refactor to avoid OOM on events to Edge
                builder.setData(ByteString.copyFrom(otaPackage.getData().readAllBytes()));
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        return builder.build();
    }

    public OtaPackageUpdateMsg constructOtaPackageDeleteMsg(OtaPackageId otaPackageId) {
        return OtaPackageUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(otaPackageId.getId().getMostSignificantBits())
                .setIdLSB(otaPackageId.getId().getLeastSignificantBits()).build();
    }

}
