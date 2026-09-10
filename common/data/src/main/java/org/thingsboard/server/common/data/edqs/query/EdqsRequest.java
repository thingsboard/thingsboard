// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EdqsRequest {

    private EntityDataQuery entityDataQuery;
    private EntityCountQuery entityCountQuery;

}
