// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.cmd;

import lombok.Data;

import java.util.List;

@Data
public class CmdsWrapper {

    private List<EntityDataCmd> entityDataCmds;

}
