// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import lombok.Data;

import java.util.Map;

@Data
public class IntegrationMetaData {

    private final Map<String, String> kvMap;

}
