// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.script.ScriptLanguage;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DedicatedConverterConfig {
    private EntityType type;
    private String name;
    private String label;
    private String profile;
    private String customer;
    private String group;
    private Set<String> attributes;
    private Set<String> telemetry;
}
