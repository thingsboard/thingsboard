// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.List;

@Slf4j
@Data
public class LwM2mRPkCredentials {
    private PublicKey serverPublicKey;
    private PrivateKey serverPrivateKey;
    private X509Certificate certificate;
    private List<Certificate> trustStore;

    /**
     * create All key RPK credentials
     * @param publX
     * @param publY
     * @param privS
     */
    public LwM2mRPkCredentials(String publX, String publY, String privS) {
        generatePublicKeyRPK(publX, publY, privS);
    }

    private void generatePublicKeyRPK(String publX, String publY, String privS) {
        try {
            /*Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
             if (publX != null && !publX.isEmpty() && publY != null && !publY.isEmpty()) {
                // Get point values
                byte[] publicX = Hex.decodeHex(publX.toCharArray());
                byte[] publicY = Hex.decodeHex(publY.toCharArray());
                 /* Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                 /* Get keys */
                this.serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            }
            if (privS != null && !privS.isEmpty()) {
                /* Get point values */
                byte[] privateS = Hex.decodeHex(privS.toCharArray());
                /* Create key specs */
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
                /* Get keys */
                this.serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server KeyRPK", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
