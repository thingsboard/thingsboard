// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.geo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntityGeofencingState {

    private boolean inside;
    private long stateSwitchTime;
    private boolean stayed;

}
