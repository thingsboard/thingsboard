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
package org.thingsboard.server.service.stats.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.stats.EntityStatisticsValue;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceStats implements EntityStatisticsValue {

    private DeviceClass deviceClass;
    private long dailyMsgCount;
    private long dailyDataPointsCount;

    @Override
    public EntityStatisticsValue update(EntityStatisticsValue newValue) {
        DeviceStats newStats = (DeviceStats) newValue;

        DeviceStats updatedStats = new DeviceStats();
        if (newStats.getDeviceClass().isHigherThan(deviceClass)) {
            updatedStats.setDeviceClass(newStats.getDeviceClass());
        } else {
            // ignoring device class downgrades
            updatedStats.setDeviceClass(deviceClass);
        }
        updatedStats.setDailyMsgCount(newStats.getDailyMsgCount());
        updatedStats.setDailyDataPointsCount(newStats.getDailyDataPointsCount());
        return updatedStats;
    }

}
