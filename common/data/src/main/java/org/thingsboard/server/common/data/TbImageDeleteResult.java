// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.wl.WhiteLabeling;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TbImageDeleteResult {

    private boolean success;
    private List<WhiteLabeling> whiteLabelingList;
    private Map<String, List<? extends HasId<?>>> references;

}
