// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.stream.Stream;

@Schema
@Data
@AllArgsConstructor
public class LwM2mResourceObserve {
    @Schema(description = "LwM2M Resource Observe id.", example = "0")
    int id;
    @Schema(description = "LwM2M Resource Observe name.", example = "Data")
    String name;
    @Schema(description = "LwM2M Resource Observe observe.", example = "false")
    boolean observe;
    @Schema(description = "LwM2M Resource Observe attribute.", example = "false")
    boolean attribute;
    @Schema(description = "LwM2M Resource Observe telemetry.", example = "false")
    boolean telemetry;
    @Schema(description = "LwM2M Resource Observe key name.", example = "data")
    String keyName;

    public LwM2mResourceObserve(int id, String name, boolean observe, boolean attribute, boolean telemetry) {
        this.id = id;
        this.name = name;
        this.observe = observe;
        this.attribute = attribute;
        this.telemetry = telemetry;
        this.keyName = getCamelCase (this.name);
    }

    private String getCamelCase (String name) {
        name = name.replaceAll("-", " ");
        name = name.replaceAll("_", " ");
        String [] nameCamel1 = name.split(" ");
        String [] nameCamel2 = new String[nameCamel1.length];
        int[] idx = { 0 };
        Stream.of(nameCamel1).forEach((s -> {
            nameCamel2[idx[0]] = toProperCase(idx[0]++,  s);
        }));
        return String.join("", nameCamel2);
    }

    private String toProperCase(int idx, String s) {
        if (!s.isEmpty() && s.length()> 0) {
            String s1 = (idx == 0) ? s.substring(0, 1).toLowerCase() : s.substring(0, 1).toUpperCase();
            String s2 = "";
            if (s.length()> 1) s2 = s.substring(1).toLowerCase();
            s = s1 + s2;
        }
        return s;
    }
}
