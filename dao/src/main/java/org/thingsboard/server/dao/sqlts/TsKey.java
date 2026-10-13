// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sqlts;

import lombok.Data;

import java.util.UUID;

@Data
public class TsKey {
    private final UUID entityId;
    private final int key;
}
