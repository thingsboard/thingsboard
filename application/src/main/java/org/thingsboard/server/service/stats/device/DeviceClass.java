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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DeviceClass {

    S(240, 24),
    M(3_000, 300),
    L(100_000, 10_000),
    XL(Long.MAX_VALUE, Long.MAX_VALUE);

    private final long maxDailyDataPointsCount;
    private final long maxDailyMsgCount;

    public boolean matches(long dailyMsgCount, long dailyDataPointsCount) {
        return dailyMsgCount <= maxDailyMsgCount && dailyDataPointsCount <= maxDailyDataPointsCount;
    }

    public boolean isHigherThan(DeviceClass otherClass) {
        return ordinal() > otherClass.ordinal();
    }

    public static DeviceClass defineClass(long dailyMsgCount, long dailyDataPointsCount) {
        for (DeviceClass deviceClass : values()) {
            if (deviceClass.matches(dailyMsgCount, dailyDataPointsCount)) {
                return deviceClass;
            }
        }
        throw new IllegalStateException("Couldn't define class");
    }

}
