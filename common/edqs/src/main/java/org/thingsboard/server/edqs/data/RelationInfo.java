// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import lombok.Data;

@Data
public class RelationInfo {

    private final String type;
    private final EntityData<?> target;

}
