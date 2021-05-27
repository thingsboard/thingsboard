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
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.firmware.FirmwareDataCache;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.firmware.ChecksumAlgorithm;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.FIRMWARE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseFirmwareService implements FirmwareService {
    public static final String INCORRECT_FIRMWARE_ID = "Incorrect firmwareId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final TenantDao tenantDao;
    private final DeviceProfileDao deviceProfileDao;
    private final FirmwareDao firmwareDao;
    private final FirmwareInfoDao firmwareInfoDao;
    private final CacheManager cacheManager;
    private final FirmwareDataCache firmwareDataCache;

    @Override
    public FirmwareInfo saveFirmwareInfo(FirmwareInfo firmwareInfo) {
        log.trace("Executing saveFirmwareInfo [{}]", firmwareInfo);
        firmwareInfoValidator.validate(firmwareInfo, FirmwareInfo::getTenantId);
        try {
            FirmwareId firmwareId = firmwareInfo.getId();
            if (firmwareId != null) {
                Cache cache = cacheManager.getCache(FIRMWARE_CACHE);
                cache.evict(toFirmwareInfoKey(firmwareId));
                firmwareDataCache.evict(firmwareId.toString());
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
                Cache cache = cacheManager.getCache(FIRMWARE_CACHE);
                cache.evict(toFirmwareInfoKey(firmwareId));
                firmwareDataCache.evict(firmwareId.toString());
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
    public String generateChecksum(ChecksumAlgorithm checksumAlgorithm, ByteBuffer data) {
        if (data == null || !data.hasArray() || data.array().length == 0) {
            throw new DataValidationException("Firmware data should be specified!");
        }

        return getHashFunction(checksumAlgorithm).hashBytes(data.array()).toString();
    }

    private HashFunction getHashFunction(ChecksumAlgorithm checksumAlgorithm) {
        switch (checksumAlgorithm) {
            case MD5:
                return Hashing.md5();
            case SHA256:
                return Hashing.sha256();
            case SHA384:
                return Hashing.sha384();
            case SHA512:
                return Hashing.sha512();
            case CRC32:
                return Hashing.crc32();
            case MURMUR3_32:
                return Hashing.murmur3_32();
            case MURMUR3_128:
                return Hashing.murmur3_128();
            default:
                throw new DataValidationException("Unknown checksum algorithm!");
        }
    }

    @Override
    public Firmware findFirmwareById(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing findFirmwareById [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        return firmwareDao.findById(tenantId, firmwareId.getId());
    }

    @Override
    @Cacheable(cacheNames = FIRMWARE_CACHE, key = "{#firmwareId}")
    public FirmwareInfo findFirmwareInfoById(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing findFirmwareInfoById [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        return firmwareInfoDao.findById(tenantId, firmwareId.getId());
    }

    @Override
    public ListenableFuture<FirmwareInfo> findFirmwareInfoByIdAsync(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing findFirmwareInfoByIdAsync [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        return firmwareInfoDao.findByIdAsync(tenantId, firmwareId.getId());
    }

    @Override
    public PageData<FirmwareInfo> findTenantFirmwaresByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantFirmwaresByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return firmwareInfoDao.findFirmwareInfoByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<FirmwareInfo> findTenantFirmwaresByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, FirmwareType firmwareType, boolean hasData, PageLink pageLink) {
        log.trace("Executing findTenantFirmwaresByTenantIdAndHasData, tenantId [{}], hasData [{}] pageLink [{}]", tenantId, hasData, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return firmwareInfoDao.findFirmwareInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, firmwareType, hasData, pageLink);
    }

    @Override
    public void deleteFirmware(TenantId tenantId, FirmwareId firmwareId) {
        log.trace("Executing deleteFirmware [{}]", firmwareId);
        validateId(firmwareId, INCORRECT_FIRMWARE_ID + firmwareId);
        try {
            Cache cache = cacheManager.getCache(FIRMWARE_CACHE);
            cache.evict(toFirmwareInfoKey(firmwareId));
            firmwareDataCache.evict(firmwareId.toString());
            firmwareDao.removeById(tenantId, firmwareId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device")) {
                throw new DataValidationException("The firmware referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device_profile")) {
                throw new DataValidationException("The firmware referenced by the device profile cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device")) {
                throw new DataValidationException("The software referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device_profile")) {
                throw new DataValidationException("The software referenced by the device profile cannot be deleted!");
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
        protected void validateDataImpl(TenantId tenantId, FirmwareInfo firmwareInfo) {
            validateImpl(firmwareInfo);
        }

        @Override
        protected void validateUpdate(TenantId tenantId, FirmwareInfo firmware) {
            FirmwareInfo firmwareOld = firmwareInfoDao.findById(tenantId, firmware.getUuidId());

            validateUpdateDeviceProfile(firmware, firmwareOld);
            BaseFirmwareService.validateUpdate(firmware, firmwareOld);
        }
    };

    private DataValidator<Firmware> firmwareValidator = new DataValidator<>() {

        @Override
        protected void validateDataImpl(TenantId tenantId, Firmware firmware) {
            validateImpl(firmware);

            if (StringUtils.isEmpty(firmware.getFileName())) {
                throw new DataValidationException("Firmware file name should be specified!");
            }

            if (StringUtils.isEmpty(firmware.getContentType())) {
                throw new DataValidationException("Firmware content type should be specified!");
            }

            if (firmware.getChecksumAlgorithm() == null) {
                throw new DataValidationException("Firmware checksum algorithm should be specified!");
            }
            if (StringUtils.isEmpty(firmware.getChecksum())) {
                throw new DataValidationException("Firmware checksum should be specified!");
            }

            String currentChecksum;

            currentChecksum = generateChecksum(firmware.getChecksumAlgorithm(), firmware.getData());

            if (!currentChecksum.equals(firmware.getChecksum())) {
                throw new DataValidationException("Wrong firmware file!");
            }
        }

        @Override
        protected void validateUpdate(TenantId tenantId, Firmware firmware) {
            Firmware firmwareOld = firmwareDao.findById(tenantId, firmware.getUuidId());

            validateUpdateDeviceProfile(firmware, firmwareOld);
            BaseFirmwareService.validateUpdate(firmware, firmwareOld);

            if (firmwareOld.getData() != null && !firmwareOld.getData().equals(firmware.getData())) {
                throw new DataValidationException("Updating firmware data is prohibited!");
            }
        }
    };

    private void validateUpdateDeviceProfile(FirmwareInfo firmware, FirmwareInfo firmwareOld) {
        if (firmwareOld.getDeviceProfileId() != null && !firmwareOld.getDeviceProfileId().equals(firmware.getDeviceProfileId())) {
            if (firmwareInfoDao.isFirmwareUsed(firmwareOld.getId(), firmware.getType(), firmwareOld.getDeviceProfileId())) {
                throw new DataValidationException("Can`t update deviceProfileId because firmware is already in use!");
            }
        }
    }

    private static void validateUpdate(FirmwareInfo firmware, FirmwareInfo firmwareOld) {
        if (!firmwareOld.getType().equals(firmware.getType())) {
            throw new DataValidationException("Updating type is prohibited!");
        }

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

        if (firmwareOld.getDataSize() != null && !firmwareOld.getDataSize().equals(firmware.getDataSize())) {
            throw new DataValidationException("Updating firmware data size is prohibited!");
        }
    }

    private void validateImpl(FirmwareInfo firmwareInfo) {
        if (firmwareInfo.getTenantId() == null) {
            throw new DataValidationException("Firmware should be assigned to tenant!");
        } else {
            Tenant tenant = tenantDao.findById(firmwareInfo.getTenantId(), firmwareInfo.getTenantId().getId());
            if (tenant == null) {
                throw new DataValidationException("Firmware is referencing to non-existent tenant!");
            }
        }

        if (firmwareInfo.getDeviceProfileId() != null) {
            DeviceProfile deviceProfile = deviceProfileDao.findById(firmwareInfo.getTenantId(), firmwareInfo.getDeviceProfileId().getId());
            if (deviceProfile == null) {
                throw new DataValidationException("Firmware is referencing to non-existent device profile!");
            }
        }

        if (firmwareInfo.getType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (StringUtils.isEmpty(firmwareInfo.getTitle())) {
            throw new DataValidationException("Firmware title should be specified!");
        }

        if (StringUtils.isEmpty(firmwareInfo.getVersion())) {
            throw new DataValidationException("Firmware version should be specified!");
        }
    }

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

    private static List<FirmwareId> toFirmwareInfoKey(FirmwareId firmwareId) {
        return Collections.singletonList(firmwareId);
    }

}
