// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThingParkRequestParameters {

    private String asId;
    private String lrnDevEui;
    private String lrnFPort;
    private String lrnInfos;
    private String time;
    private String token;

}
