// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.query.EntityFilter;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EntityAlias {

    String id;
    String alias;
    EntityFilter filter;

}
