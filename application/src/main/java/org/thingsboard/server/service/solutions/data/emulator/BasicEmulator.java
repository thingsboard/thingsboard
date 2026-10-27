// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.values.TelemetryGenerator;
import org.thingsboard.server.service.solutions.data.values.TelemetryGeneratorFactory;

import java.util.HashMap;
import java.util.Map;

public class BasicEmulator implements SimpleEmulator {

    private final Map<String, TelemetryGenerator> tsGenerators = new HashMap<>();

    @Override
    public void init(EmulatorDefinition emulatorDefinition) {
        emulatorDefinition.getTelemetryProfiles().forEach(tp -> tsGenerators.put(tp.getKey(), TelemetryGeneratorFactory.create(tp)));
    }

    @Override
    public ObjectNode getValue(long ts) {
        ObjectNode values = JacksonUtil.newObjectNode();
        tsGenerators.values().forEach(gen -> gen.addValue(ts, values));
        return values;
    }
}
