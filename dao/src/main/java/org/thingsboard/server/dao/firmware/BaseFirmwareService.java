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
package org.thingsboard.server.dao.firmware;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.FIRMWARE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class BaseFirmwareService implements FirmwareService {
    public static final String INCORRECT_FIRMWARE_ID = "Incorrect firmwareId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final TenantDao tenantDao;
    private final FirmwareDao firmwareDao;
    private final FirmwareInfoDao firmwareInfoDao;
    private final CacheManager cacheManager;

    public BaseFirmwareService(TenantDao tenantDao, FirmwareDao firmwareDao, FirmwareInfoDao firmwareInfoDao, CacheManager cacheManager) {
        this.tenantDao = tenantDao;
        this.firmwareDao = firmwareDao;
        this.firmwareInfoDao = firmwareInfoDao;
        this.cacheManager = cacheManager;
    }

    @Override
    public FirmwareInfo saveFirmwareInfo(FirmwareInfo firmwareInfo) {
        log.trace("Executing saveFirmwareInfo [{}]", firmwareInfo);
        firmwareInfoValidator.validate(firmwareInfo, FirmwareInfo::getTenantId);
        try {
            FirmwareId firmwareId = firmwareInfo.getId();
            if (firmwareId != null) {
                cacheManager.getCache(FIRMWARE_CACHE).evict(firmwareId.toString());
            }
            return firmwareInfoDao.save(firmwareInfo.getTenantId(), firmwareInfo);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("firmware_tenant_title_version_unq_key")) {
                throw new DataValidationException("Firmware with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public Firmware saveFirmware(Firmware firmware) {
        log.trace("Executing saveFirmware [{}]", firmware);
        firmwareValidator.validate(firmware, FirmwareInfo::getTenantId);
        try {
            FirmwareId firmwareId = firmware.getId();
            if (firmwareId != null) {
                cacheManager.getCache(FIRMWARE_CACHE).evict(firmwareId.toString());
            }
            return firmwareDao.save(firmware.getTenantId(), firmware);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("firmware_tenant_title_version_unq_key")) {
                throw new DataValidationException("Firmware with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public Firmware findFirmwareById(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing findFirmwareById [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        return firmwareDao.findById(tenantId, firmwareId.getId());
    }

    @Override
    public FirmwareInfo findFirmwareInfoById(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing findFirmwareInfoById [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        return firmwareInfoDao.findById(tenantId, firmwareId.getId());
    }

    @Override
    public PageData<FirmwareInfo> findTenantFirmwaresByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantFirmwaresByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return firmwareInfoDao.findFirmwareInfoByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<FirmwareInfo> findTenantFirmwaresByTenantIdAndHasData(TenantId tenantId, boolean hasData, PageLink pageLink) {
        log.trace("Executing findTenantFirmwaresByTenantIdAndHasData, tenantId [{}], hasData [{}] pageLink [{}]", tenantId, hasData, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return firmwareInfoDao.findFirmwareInfoByTenantIdAndHasData(tenantId, hasData, pageLink);
    }

    @Override
    public void deleteFirmware(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing deleteFirmware [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        try {
            Cache cache = cacheManager.getCache(FIRMWARE_CACHE);
            cache.evict(Collections.singletonList(firmwareId));
            firmwareDao.removeById(tenantId, firmwareId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device")) {
                throw new DataValidationException("The firmware referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device_profile")) {
                throw new DataValidationException("The firmware referenced by the device profile cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public void deleteFirmwaresByTenantId(TenantId tenantId) {
        log.trace("Executing deleteFirmwaresByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantFirmwareRemover.removeEntities(tenantId, tenantId);
    }

    private DataValidator<FirmwareInfo> firmwareInfoValidator = new DataValidator<>() {

        @Override
        protected void validateDataImpl(TenantId tenantId, FirmwareInfo firmware) {
            if (firmware.getTenantId() == null) {
                throw new DataValidationException("Firmware should be assigned to tenant!");
            } else {
                Tenant tenant = tenantDao.findById(firmware.getTenantId(), firmware.getTenantId().getId());
                if (tenant == null) {
                    throw new DataValidationException("Firmware is referencing to non-existent tenant!");
                }
            }

            if (StringUtils.isEmpty(firmware.getTitle())) {
                throw new DataValidationException("Firmware title should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getVersion())) {
                throw new DataValidationException("Firmware version should be specified!");
            }
        }

        @Override
        protected void validateUpdate(TenantId tenantId, FirmwareInfo firmware) {
            FirmwareInfo firmwareOld = firmwareInfoDao.findById(tenantId, firmware.getUuidId());

            if (!firmwareOld.getTitle().equals(firmware.getTitle())) {
                throw new DataValidationException("Updating firmware title is prohibited!");
            }

            if (!firmwareOld.getVersion().equals(firmware.getVersion())) {
                throw new DataValidationException("Updating firmware version is prohibited!");
            }
        }
    };

    private DataValidator<Firmware> firmwareValidator = new DataValidator<>() {

        @Override
        protected void validateDataImpl(TenantId tenantId, Firmware firmware) {
            if (firmware.getTenantId() == null) {
                throw new DataValidationException("Firmware should be assigned to tenant!");
            } else {
                Tenant tenant = tenantDao.findById(firmware.getTenantId(), firmware.getTenantId().getId());
                if (tenant == null) {
                    throw new DataValidationException("Firmware is referencing to non-existent tenant!");
                }
            }

            if (StringUtils.isEmpty(firmware.getTitle())) {
                throw new DataValidationException("Firmware title should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getVersion())) {
                throw new DataValidationException("Firmware version should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getFileName())) {
                throw new DataValidationException("Firmware file name should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getContentType())) {
                throw new DataValidationException("Firmware content type should be specified!");
            }

            ByteBuffer data = firmware.getData();
            if (data == null || !data.hasArray() || data.array().length == 0) {
                throw new DataValidationException("Firmware data should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getChecksumAlgorithm())) {
                throw new DataValidationException("Firmware checksum algorithm should be specified!");
            }
            if (StringUtils.isEmpty(firmware.getChecksum())) {
                throw new DataValidationException("Firmware checksum should be specified!");
            }

            HashFunction hashFunction;
            switch (firmware.getChecksumAlgorithm()) {
                case "sha256":
                    hashFunction = Hashing.sha256();
                    break;
                case "md5":
                    hashFunction = Hashing.md5();
                    break;
                case "crc32":
                    hashFunction = Hashing.crc32();
                    break;
                default:
                    throw new DataValidationException("Unknown checksum algorithm!");
            }

            String currentChecksum = hashFunction.hashBytes(data.array()).toString();

            if (!currentChecksum.equals(firmware.getChecksum())) {
                throw new DataValidationException("Wrong firmware file!");
            }
        }

        @Override
        protected void validateUpdate(TenantId tenantId, Firmware firmware) {
            Firmware firmwareOld = firmwareDao.findById(tenantId, firmware.getUuidId());

            if (!firmwareOld.getTitle().equals(firmware.getTitle())) {
                throw new DataValidationException("Updating firmware title is prohibited!");
            }

            if (!firmwareOld.getVersion().equals(firmware.getVersion())) {
                throw new DataValidationException("Updating firmware version is prohibited!");
            }

            if (firmwareOld.getFileName() != null && !firmwareOld.getFileName().equals(firmware.getFileName())) {
                throw new DataValidationException("Updating firmware file name is prohibited!");
            }

            if (firmwareOld.getContentType() != null && !firmwareOld.getContentType().equals(firmware.getContentType())) {
                throw new DataValidationException("Updating firmware content type is prohibited!");
            }

            if (firmwareOld.getChecksumAlgorithm() != null && !firmwareOld.getChecksumAlgorithm().equals(firmware.getChecksumAlgorithm())) {
                throw new DataValidationException("Updating firmware content type is prohibited!");
            }

            if (firmwareOld.getChecksum() != null && !firmwareOld.getChecksum().equals(firmware.getChecksum())) {
                throw new DataValidationException("Updating firmware content type is prohibited!");
            }

            if (firmwareOld.getData() != null && !firmwareOld.getData().equals(firmware.getData())) {
                throw new DataValidationException("Updating firmware data is prohibited!");
            }
        }
    };

    private PaginatedRemover<TenantId, FirmwareInfo> tenantFirmwareRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<FirmwareInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return firmwareInfoDao.findFirmwareInfoByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, FirmwareInfo entity) {
                    deleteFirmware(tenantId, entity.getId());
                }
            };

    protected Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }
}
