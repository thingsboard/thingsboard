// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

@Data
public abstract class TelemetryGenerator {
    protected final TelemetryProfile profile;
    protected final String key;

    public TelemetryGenerator(TelemetryProfile telemetryProfile) {
        this.profile = telemetryProfile;
        this.key = telemetryProfile.getKey();
    }

    public double getValue() {
        throw new RuntimeException("Not supported");
    }

    public void setValue(double value) {
        throw new RuntimeException("Not supported");
    }

    public abstract void addValue(long ts, ObjectNode values);
}
