// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;

@Data
@NoArgsConstructor
public class X509CertificateChainProvisionConfiguration implements DeviceProfileProvisionConfiguration {

    private String provisionDeviceSecret;
    private String certificateRegExPattern;
    private boolean allowCreateNewDevicesByX509Certificate;

    @Override
    public DeviceProfileProvisionType getType() {
        return DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN;
    }

}
