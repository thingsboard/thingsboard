// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class LwM2MLocationParams {

    private Float latitude;
    private Float longitude;
    private Float scaleFactor = 1.0F;
    private final String locationPos = "50.4501:30.5234";
    private final Float locationScaleFactor = 1.0F;


   protected void getPos() {
        this.latitude = null;
        this.longitude = null;
        int c = locationPos.indexOf(':');
        this.latitude = Float.valueOf(locationPos.substring(0, c));
        this.longitude = Float.valueOf(locationPos.substring(c + 1));
    }


}
