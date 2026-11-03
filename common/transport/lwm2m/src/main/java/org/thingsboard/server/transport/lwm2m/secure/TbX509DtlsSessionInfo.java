// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.Data;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;

import java.io.Serializable;

@Data
public class TbX509DtlsSessionInfo implements Serializable {

    private final String x509CommonName;
    private final ValidateDeviceCredentialsResponse credentials;

}
