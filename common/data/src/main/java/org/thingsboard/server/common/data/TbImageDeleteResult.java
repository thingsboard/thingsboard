// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.HasId;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TbImageDeleteResult {

    private boolean success;
    private Map<String, List<? extends HasId<?>>> references;

}
