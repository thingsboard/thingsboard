// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edqs.query;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EdqsRequest {

    private EntityDataQuery entityDataQuery;
    private EntityCountQuery entityCountQuery;
    @JsonIncludeProperties({"genericPermissions", "groupPermissions"})
    private MergedUserPermissions userPermissions;

}
