// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import lombok.Data;

@Data
public class WhiteLabelingEvictEvent {
    private final WhiteLabelingCacheKey key;
}
