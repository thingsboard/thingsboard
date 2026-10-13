// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.query.KeyFilter;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class Filter {

    String id;
    String filter;
    List<KeyFilter> keyFilters;

}
