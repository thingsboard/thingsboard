// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerRepeat;
import org.thingsboard.server.common.data.scheduler.TimerRepeat;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.common.data.DataConstants.UPDATE_FIRMWARE;
import static org.thingsboard.server.common.data.DataConstants.UPDATE_SOFTWARE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.scheduler.BaseSchedulerEventService.getOriginatorId;

@Component
public class SchedulerEventDataValidator extends DataValidator<SchedulerEvent> {

    @Value("${reports.scheduler.min_interval:60}")
    private int minTimerBasedIntervalForEventInSec;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private SchedulerEventDao schedulerEventDao;

    @Override
    protected void validateCreate(TenantId tenantId, SchedulerEvent data) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.SCHEDULER_EVENT);
    }

    @Override
    protected SchedulerEvent validateUpdate(TenantId tenantId, SchedulerEvent data) {
        SchedulerEvent old = schedulerEventDao.findById(data.getTenantId(), data.getUuidId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing entity!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, SchedulerEvent schedulerEvent) {
        if (StringUtils.isEmpty(schedulerEvent.getType())) {
            throw new DataValidationException("SchedulerEvent type should be specified!");
        }
        if (StringUtils.isEmpty(schedulerEvent.getName())) {
            throw new DataValidationException("SchedulerEvent name should be specified!");
        }
        if (schedulerEvent.getSchedule() == null) {
            throw new DataValidationException("SchedulerEvent schedule configuration should be specified!");
        }
        if (schedulerEvent.getConfiguration() == null) {
            throw new DataValidationException("SchedulerEvent configuration should be specified!");
        }
        if (schedulerEvent.getTenantId() == null) {
            throw new DataValidationException("SchedulerEvent should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(schedulerEvent.getTenantId())) {
                throw new DataValidationException("SchedulerEvent is referencing to non-existent tenant!");
            }
        }
        if (schedulerEvent.getCustomerId() == null) {
            schedulerEvent.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!schedulerEvent.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, schedulerEvent.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign schedulerEvent to non-existent customer!");
            }
            if (!customer.getTenantId().equals(schedulerEvent.getTenantId())) {
                throw new DataValidationException("Can't assign schedulerEvent to customer from different tenant!");
            }
        }

        boolean isFirmwareUpdate = UPDATE_FIRMWARE.equals(schedulerEvent.getType());
        boolean isSoftwareUpdate = UPDATE_SOFTWARE.equals(schedulerEvent.getType());

        if (isFirmwareUpdate || isSoftwareUpdate) {
            OtaPackageId firmwareId =
                    JacksonUtil.convertValue(schedulerEvent.getConfiguration().get("msgBody"), OtaPackageId.class);
            if (firmwareId == null) {
                throw new DataValidationException("SchedulerEvent firmwareId should be specified!");
            }
            OtaPackageInfo firmwareInfo = otaPackageService.findOtaPackageById(tenantId, firmwareId);
            if (firmwareInfo == null) {
                throw new DataValidationException("Can't assign non-existent firmware!");
            }

            if ((isFirmwareUpdate && !OtaPackageType.FIRMWARE.equals(firmwareInfo.getType()))
                    || (isSoftwareUpdate && !OtaPackageType.SOFTWARE.equals(firmwareInfo.getType()))) {
                throw new DataValidationException("SchedulerEvent Can't assign firmware with different type!");
            }

            EntityId originatorId = getOriginatorId(schedulerEvent);

            if (originatorId == null) {
                throw new DataValidationException("SchedulerEvent originatorId should be specified!");
            }

            switch (originatorId.getEntityType()) {
                case DEVICE:
                    Device device = deviceService.findDeviceById(tenantId, (DeviceId) originatorId);
                    if (!device.getDeviceProfileId().equals(firmwareInfo.getDeviceProfileId())) {
                        throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                    }
                    break;
                case DEVICE_PROFILE:
                    DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) originatorId);
                    if (!deviceProfile.getId().equals(firmwareInfo.getDeviceProfileId())) {
                        throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                    }
                    break;
            }
        }

        JsonNode repeatNode = schedulerEvent.getSchedule().get("repeat");
        if (repeatNode != null) {
            var repeat = JacksonUtil.treeToValue(repeatNode, SchedulerRepeat.class);
            if (repeat instanceof TimerRepeat timerRepeat) {
                long seconds = timerRepeat.getTimeUnit().toSeconds(timerRepeat.getRepeatInterval());
                if (seconds < minTimerBasedIntervalForEventInSec) {
                    throw new DataValidationException("Timer-based repeats are too frequent (less than " + minTimerBasedIntervalForEventInSec + " seconds)!");
                }
            }
        }
    }

}
