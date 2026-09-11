// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public enum AlarmSeverity {

    CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE;

    @Getter
    private final String displayName = StringUtils.capitalize(name().toLowerCase());

}
