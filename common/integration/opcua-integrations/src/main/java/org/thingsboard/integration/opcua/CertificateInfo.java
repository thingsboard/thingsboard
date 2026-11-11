// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import lombok.Data;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * Created by Valerii Sosliuk on 3/22/2018.
 */
@Data
public class CertificateInfo {

    private final X509Certificate certificate;
    private final KeyPair keyPair;

}
