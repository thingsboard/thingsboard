// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import lombok.Data;

import java.io.Serializable;

@Data
public class AlarmAssigneeUpdate implements Serializable {

    private static final long serialVersionUID = -2391676304697483808L;

    private final boolean deleted;
    private final AlarmAssignee assignee;

}
