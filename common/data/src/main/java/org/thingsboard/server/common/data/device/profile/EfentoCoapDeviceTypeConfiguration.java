// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.CoapDeviceType;

@Data
public class EfentoCoapDeviceTypeConfiguration implements CoapDeviceTypeConfiguration {

    private static final long serialVersionUID = -8523081152598707064L;

    @Override
    public CoapDeviceType getCoapDeviceType() {
        return CoapDeviceType.EFENTO;
    }
}
