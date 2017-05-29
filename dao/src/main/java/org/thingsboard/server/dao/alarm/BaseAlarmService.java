/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.page.TimePageData;

import java.util.Optional;

@Service
@Slf4j
public class BaseAlarmService implements AlarmService {

    @Override
    public Alarm findAlarmById(AlarmId alarmId) {
        return null;
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(AlarmId alarmId) {
        return null;
    }

    @Override
    public Optional<Alarm> saveIfNotExists(Alarm alarm) {
        return null;
    }

    @Override
    public ListenableFuture<Boolean> updateAlarm(Alarm alarm) {
        return null;
    }

    @Override
    public ListenableFuture<Boolean> ackAlarm(Alarm alarm) {
        return null;
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(AlarmId alarmId) {
        return null;
    }

    @Override
    public ListenableFuture<TimePageData<Alarm>> findAlarms(AlarmQuery query) {
        return null;
    }
}
