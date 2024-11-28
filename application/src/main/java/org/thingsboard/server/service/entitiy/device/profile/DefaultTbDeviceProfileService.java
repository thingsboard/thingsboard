/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.device.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.data.Column;
import org.thingsboard.server.common.data.device.data.TableInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.util.TDengineTsDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbDeviceProfileService extends AbstractTbEntityService implements TbDeviceProfileService {

    private final DeviceProfileService deviceProfileService;

    // for tdengine
    @Autowired(required = false)
    @Qualifier("TDengineTemplate")
    @TDengineTsDao
    protected JdbcTemplate jdbcTemplate;

    @Override
    public DeviceProfile save(DeviceProfile deviceProfile, SecurityUser user) throws Exception {
        ActionType actionType = deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));
            autoCommit(user, savedDeviceProfile.getId());

            logEntityActionService.logEntityAction(tenantId, savedDeviceProfile.getId(), savedDeviceProfile,
                    null, actionType, user);

            /* for tdengine, start */
            if (useStable && actionType.equals(ActionType.ADDED) && deviceProfile.getTableInfo() != null) {
                StringBuilder sb = new StringBuilder("create table `").append(savedDeviceProfile.getId()).append("` (ts timestamp, ");
                TableInfo tableInfo = deviceProfile.getTableInfo();
                sb.append(tableInfo.getColumns().stream().map(c -> {
                    String s = "`" + c.getName() + "` " + c.getType();
                    if ("BINARY".equalsIgnoreCase(c.getType()) || "VARCHAR".equalsIgnoreCase(c.getType()) || "NCHAR".equalsIgnoreCase(c.getType())) {
                        return s + "(" + c.getLen() + ")";
                    }
                    return s;
                }).collect(Collectors.joining(",")));
                sb.append(") tags (device_name varchar(255)");
                String tags = null;
                if (null != tableInfo.getTags()) {
                    tags = tableInfo.getTags().stream().map(t -> {
                        String s = "`" + t.getName() + "` " + t.getType();
                        if ("BINARY".equalsIgnoreCase(t.getType()) || "VARCHAR".equalsIgnoreCase(t.getType()) || "NCHAR".equalsIgnoreCase(t.getType())) {
                            return s + "(" + t.getLen() + ")";
                        }
                        return s;
                    }).collect(Collectors.joining(","));
                }
                if (!Strings.isBlank(tags)) {
                    sb.append(",").append(tags);
                }
                sb.append(") comment '").append(deviceProfile.getName()).append("'");
                log.info("create stable for device profile:{} using sql:{}", deviceProfile.getName(), sb);
                jdbcTemplate.execute(sb.toString());
            }
            if (useStable) {
                TableInfo visualInfo = new TableInfo();
                jdbcTemplate.query("desc `" + savedDeviceProfile.getId() + "`", rs -> {
                    List<Column> columns = new ArrayList<>();
                    List<Column> tags = new ArrayList<>();
                    do {
                        Column column = new Column();
                        column.setName(rs.getString("field"));
                        column.setType(rs.getString("type"));
                        column.setLen(rs.getInt("length"));
                        if (StringUtils.isNotEmpty(rs.getString("note"))) {
                            tags.add(column);
                        } else {
                            columns.add(column);
                        }
                    } while (rs.next());
                    visualInfo.setColumns(columns);
                    visualInfo.setTags(tags);
                });
                savedDeviceProfile.setTableInfo(visualInfo);
            }
            /* for tdengine, end */
            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), deviceProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(DeviceProfile deviceProfile, User user) {
        ActionType actionType = ActionType.DELETED;
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);

            logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, null,
                    actionType, user, deviceProfileId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), actionType,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }

    @Override
    public DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException {
        TenantId tenantId = deviceProfile.getTenantId();
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            if (deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, previousDefaultDeviceProfile.getId());
                    logEntityActionService.logEntityAction(tenantId, previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            ActionType.UPDATED, user);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);

                logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, ActionType.UPDATED, user);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), ActionType.UPDATED,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }
}
