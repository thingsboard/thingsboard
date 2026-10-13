// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CustomMenuDeleteResult {

    private boolean success;
    private CMAssigneeType assigneeType;
    private List<EntityInfo> assigneeList;

}
