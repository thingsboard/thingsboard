// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class ParametersUpdateAnalyzeResult {
    ParametersAnalyzeResult analyzerParameters;
    Set<String> newObjectsToRead;
    Set<String> newObjectsToCancelRead;
    Map<String, ObjectAttributes> attributeLwm2mNew;
}

