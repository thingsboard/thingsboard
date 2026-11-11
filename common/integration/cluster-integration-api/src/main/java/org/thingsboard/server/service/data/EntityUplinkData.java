// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.data;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class EntityUplinkData {

    @NonNull
    private final String name;
    @NonNull
    private final String type;
    @NonNull
    @Builder.Default
    private final String label = "";
    @NonNull
    @Builder.Default
    private final String customerName = "";
    @NonNull
    @Builder.Default
    private final String groupName = "";

}
