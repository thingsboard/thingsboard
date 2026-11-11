// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceInfoDao;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;
import org.thingsboard.server.dao.sql.CitusSubCustomerInfoQuery;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaDeviceInfoDao extends JpaAbstractDao<DeviceInfoEntity, DeviceInfo> implements DeviceInfoDao {

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Autowired
    private CitusSubCustomerInfoQuery citusSubCustomerInfoQuery;

    @Override
    protected Class<DeviceInfoEntity> getEntityClass() {
        return DeviceInfoEntity.class;
    }

    @Override
    protected JpaRepository<DeviceInfoEntity, UUID> getRepository() {
        return deviceInfoRepository;
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        if (filter.getCustomerId() != null && filter.isIncludeCustomers()) {
            UUID tenantId = filter.getTenantId().getId();
            UUID customerId = filter.getCustomerId().getId();
            UUID deviceProfileId = DaoUtil.getId(filter.getDeviceProfileId());
            boolean filterByActive = filter.getActive() != null;
            boolean deviceActive = Boolean.TRUE.equals(filter.getActive());
            String textSearch = pageLink.getTextSearch();
            Pageable pageable = DaoUtil.toPageable(pageLink);
            return citusSubCustomerInfoQuery.findIncludingSubCustomers(
                    tenantId, customerId,
                    customerIds -> deviceInfoRepository.findDeviceInfosByFilterInCustomerIds(
                            tenantId, customerId, customerIds, deviceProfileId, filterByActive, deviceActive, textSearch, pageable),
                    () -> deviceInfoRepository.findDeviceInfosByFilterIncludingSubCustomers(
                            tenantId, customerId, deviceProfileId, filterByActive, deviceActive, textSearch, pageable));
        } else {
            return DaoUtil.toPageData(
                    deviceInfoRepository.findDeviceInfosByFilter(
                            filter.getTenantId().getId(),
                            filter.isIncludeCustomers(),
                            DaoUtil.getId(filter.getCustomerId()),
                            DaoUtil.getId(filter.getDeviceProfileId()),
                            filter.getActive() != null,
                            Boolean.TRUE.equals(filter.getActive()),
                            pageLink.getTextSearch(),
                            DaoUtil.toPageable(pageLink)));
        }
    }

}
