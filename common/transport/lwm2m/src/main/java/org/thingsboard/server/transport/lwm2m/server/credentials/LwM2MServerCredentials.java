/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server.credentials;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.List;

@Slf4j
@Data
public class LwM2MServerCredentials {
    PublicKey serverPublicKey;
    PrivateKey serverPrivateKey;
    X509Certificate certificate;
    List<Certificate> trustStore;

    /**
     * create server RPK credentials
     * @param publX
     * @param publY
     * @param privS
     */
    public LwM2MServerCredentials(String publX, String publY, String privS) {
        generateServerKeyRPK(publX, publY, privS);
//        log.info("spublk_X: [{}] [{}]",  publX.toUpperCase(), publX.length());
//        log.info("spublk_Y: [{}] [{}]",  publY.toUpperCase(), publY.length());
//        log.info("spublk: [{}]",  Hex.encodeHexString(this.serverPublicKey.getEncoded()).toUpperCase());
//        log.info("spriv_S: [{}] [{}]",  privS.toUpperCase(), privS.length());
//        log.info("sprivk: [{}]",  Hex.encodeHexString(this.serverPrivateKey.getEncoded()).toUpperCase());
    }

    private void generateServerKeyRPK(String publX, String publY, String privS) {
        try {
            // Get point values
            byte[] publicX = Hex.decodeHex(publX.toCharArray());
            byte[] publicY = Hex.decodeHex(publY.toCharArray());
            byte[] privateS = Hex.decodeHex(privS.toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            this.serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            this.serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
