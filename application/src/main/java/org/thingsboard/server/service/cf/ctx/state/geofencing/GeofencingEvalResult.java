// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import jakarta.annotation.Nullable;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent;

public record GeofencingEvalResult(@Nullable GeofencingTransitionEvent transition,
                                   GeofencingPresenceStatus status) {
}
