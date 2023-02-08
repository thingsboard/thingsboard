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
package org.thingsboard.server.dao.ota;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseOtaPackageService extends AbstractCachedEntityService<OtaPackageCacheKey, OtaPackageInfo, OtaPackageCacheEvictEvent> implements OtaPackageService {
    public static final String INCORRECT_OTA_PACKAGE_ID = "Incorrect otaPackageId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final OtaPackageDao otaPackageDao;
    private final OtaPackageInfoDao otaPackageInfoDao;
    private final OtaPackageDataCache otaPackageDataCache;
    private final DataValidator<OtaPackageInfo> otaPackageInfoValidator;
    private final DataValidator<OtaPackage> otaPackageValidator;

    @TransactionalEventListener(classes = OtaPackageCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(OtaPackageCacheEvictEvent event) {
        cache.evict(new OtaPackageCacheKey(event.getId()));
        otaPackageDataCache.evict(event.getId().toString());
    }

    @Override
    public OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl) {
        log.trace("Executing saveOtaPackageInfo [{}]", otaPackageInfo);
        if (isUrl && (StringUtils.isEmpty(otaPackageInfo.getUrl()) || otaPackageInfo.getUrl().trim().length() == 0)) {
            throw new DataValidationException("Ota package URL should be specified!");
        }
        otaPackageInfoValidator.validate(otaPackageInfo, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            var result = otaPackageInfoDao.save(otaPackageInfo.getTenantId(), otaPackageInfo);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public OtaPackage saveOtaPackage(OtaPackage otaPackage) {
        log.trace("Executing saveOtaPackage [{}]", otaPackage);
        otaPackageValidator.validate(otaPackage, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackage.getId();
        try {
            var result = otaPackageDao.save(otaPackage.getTenantId(), otaPackage);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public String generateChecksum(ChecksumAlgorithm checksumAlgorithm, ByteBuffer data) {
        if (data == null || !data.hasArray() || data.array().length == 0) {
            throw new DataValidationException("OtaPackage data should be specified!");
        }

        return getHashFunction(checksumAlgorithm).hashBytes(data.array()).toString();
    }

    @SuppressWarnings("deprecation")
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
    public OtaPackage findOtaPackageById(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageDao.findById(tenantId, otaPackageId.getId());
    }

    @Override
    public OtaPackageInfo findOtaPackageInfoById(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return cache.getAndPutInTransaction(new OtaPackageCacheKey(otaPackageId),
                () -> otaPackageInfoDao.findById(tenantId, otaPackageId.getId()), true);
    }

    @Override
    public ListenableFuture<OtaPackageInfo> findOtaPackageInfoByIdAsync(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoByIdAsync [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageInfoDao.findByIdAsync(tenantId, otaPackageId.getId());
    }

    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantIdAndHasData, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, otaPackageType, pageLink);
    }

    @Override
    public void deleteOtaPackage(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing deleteOtaPackage [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        try {
            otaPackageDao.removeById(tenantId, otaPackageId.getId());
            publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device")) {
                throw new DataValidationException("The otaPackage referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device_profile")) {
                throw new DataValidationException("The otaPackage referenced by the device profile cannot be deleted!");
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
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageDao.sumDataSizeByTenantId(tenantId);
    }

    @Override
    public void deleteOtaPackagesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteOtaPackagesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantOtaPackageRemover.removeEntities(tenantId, tenantId);
    }

    private PaginatedRemover<TenantId, OtaPackageInfo> tenantOtaPackageRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<OtaPackageInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return otaPackageInfoDao.findOtaPackageInfoByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, OtaPackageInfo entity) {
                    deleteOtaPackage(tenantId, entity.getId());
                }
            };

}
