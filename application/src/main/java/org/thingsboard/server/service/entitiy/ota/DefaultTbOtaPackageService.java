/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.ota;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.nio.ByteBuffer;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbOtaPackageService extends AbstractTbEntityService implements TbOtaPackageService {

    private final OtaPackageService otaPackageService;

    @Override
    public OtaPackageInfo save(SaveOtaPackageInfoRequest saveOtaPackageInfoRequest, User user) throws ThingsboardException {
        ActionType actionType = saveOtaPackageInfoRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = saveOtaPackageInfoRequest.getTenantId();
        try {
            OtaPackageInfo savedOtaPackageInfo = otaPackageService.saveOtaPackageInfo(new OtaPackageInfo(saveOtaPackageInfoRequest), saveOtaPackageInfoRequest.isUsesUrl());

            boolean sendMsgToEdge = savedOtaPackageInfo.hasUrl() || savedOtaPackageInfo.isHasData();
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedOtaPackageInfo.getId(),
                    savedOtaPackageInfo, user, actionType, sendMsgToEdge, null);

            return savedOtaPackageInfo;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE), saveOtaPackageInfoRequest,
                    actionType, user, e);
            throw e;
        }
    }

    @Override
    public OtaPackageInfo saveOtaPackageData(OtaPackageInfo otaPackageInfo, String checksum, ChecksumAlgorithm checksumAlgorithm,
                                             byte[] data, String filename, String contentType, User user) throws ThingsboardException {
        TenantId tenantId = otaPackageInfo.getTenantId();
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            if (StringUtils.isEmpty(checksum)) {
                checksum = otaPackageService.generateChecksum(checksumAlgorithm, ByteBuffer.wrap(data));
            }
            OtaPackage otaPackage = new OtaPackage(otaPackageId);
            otaPackage.setCreatedTime(otaPackageInfo.getCreatedTime());
            otaPackage.setTenantId(tenantId);
            otaPackage.setDeviceProfileId(otaPackageInfo.getDeviceProfileId());
            otaPackage.setType(otaPackageInfo.getType());
            otaPackage.setTitle(otaPackageInfo.getTitle());
            otaPackage.setVersion(otaPackageInfo.getVersion());
            otaPackage.setTag(otaPackageInfo.getTag());
            otaPackage.setAdditionalInfo(otaPackageInfo.getAdditionalInfo());
            otaPackage.setChecksumAlgorithm(checksumAlgorithm);
            otaPackage.setChecksum(checksum);
            otaPackage.setFileName(filename);
            otaPackage.setContentType(contentType);
            otaPackage.setData(ByteBuffer.wrap(data));
            otaPackage.setDataSize((long) data.length);
            OtaPackageInfo savedOtaPackage = otaPackageService.saveOtaPackage(otaPackage);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedOtaPackage.getId(),
                    savedOtaPackage, user, ActionType.UPDATED, true, null);
            return savedOtaPackage;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE), ActionType.UPDATED,
                    user, e, otaPackageId.toString());
            throw e;
        }
    }

    @Override
    public void delete(OtaPackageInfo otaPackageInfo, User user) throws ThingsboardException {
        TenantId tenantId = otaPackageInfo.getTenantId();
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            otaPackageService.deleteOtaPackage(tenantId, otaPackageId);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, otaPackageId, otaPackageInfo,
                    user, ActionType.DELETED, true, null, otaPackageInfo.getId().toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE),
                    ActionType.DELETED, user, e, otaPackageId.toString());
            throw e;
        }
    }
}
