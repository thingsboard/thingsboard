/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class FieldIrrigationStateEmulator implements CustomEmulator {

    private static final int START_HOUR = 6;
    private int idx;
    private List<Pair<Long, ObjectNode>> data = new ArrayList<>();

    @Override
    public void init(EmulatorDefinition emulatorDefinition) {
        idx = 0;
        var tz = TimeZone.getTimeZone("America/New_York");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.setTimeZone(tz);
        int curHour = c.get(Calendar.HOUR_OF_DAY);
        c.add(Calendar.DAY_OF_MONTH, curHour < START_HOUR ? -7 : -6);
        c.set(Calendar.HOUR_OF_DAY, START_HOUR);
        c.set(Calendar.MINUTE, 0);

        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("consumption", 423)
                .put("duration", 30 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("consumption", 407)
                .put("duration", 30 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 1000)
                .put("consumption", 1000)
                .put("duration", 63 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 500)
                .put("consumption", 500)
                .put("duration", 36 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 1000)
                .put("consumption", 1000)
                .put("duration", 63 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("duration", 30 * 60000)
                .put("consumption", 452)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("duration", 30 * 60000)
                .put("consumption", 447)
        );
    }

    private void add(Calendar c, ObjectNode objectNode) {
        ObjectNode startIrrigationMsg = JacksonUtil.newObjectNode();
        startIrrigationMsg.put("irrigationState", "DONE");
        startIrrigationMsg.set("irrigationTask", objectNode);
        data.add(Pair.of(c.getTimeInMillis(), startIrrigationMsg));
        c.add(Calendar.DAY_OF_MONTH, 1);
    }

    @Override
    public Pair<Long, ObjectNode> getNextValue() {
        if (idx < data.size()) {
            var result = data.get(idx);
            idx++;
            return result;
        } else {
            return null;
        }
    }
}
