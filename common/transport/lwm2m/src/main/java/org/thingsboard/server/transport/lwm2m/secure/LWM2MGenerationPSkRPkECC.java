/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import java.security.SecureRandom;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidAlgorithmParameterException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

@Slf4j
public class LWM2MGenerationPSkRPkECC {

    public LWM2MGenerationPSkRPkECC(Integer dtlsMode) {
        switch (LwM2MSecurityMode.fromSecurityMode(dtlsMode)) {
            case PSK:
                generationPSkKey();
                break;
            case RPK:
                generationRPKECCKey();
        }
    }

    public LWM2MGenerationPSkRPkECC() {
        generationPSkKey();
        generationRPKECCKey();
    }

    private void generationPSkKey() {
        /** PSK */
        int lenPSkKey = 32;
        /** Start PSK
         * Clients and Servers MUST support PSK keys of up to 64 bytes in length, as required by [RFC7925]
         * SecureRandom object must be unpredictable, and all SecureRandom output sequences must be cryptographically strong, as described in [RFC4086]
         * */
        SecureRandom randomPSK = new SecureRandom();
        byte bytesPSK[] = new byte[lenPSkKey];
        randomPSK.nextBytes(bytesPSK);
        log.info("\nCreating new PSK: \n for the next start PSK -> security key: [{}]", Hex.encodeHexString(bytesPSK));
    }

    private void generationRPKECCKey() {
        /** RPK */
        String algorithm = "EC";
        String provider = "SunEC";
        String nameParameterSpec = "secp256r1";

        /** Start RPK
         * Elliptic Curve parameters  : [secp256r1 [NIST P-256, X9.62 prime256v1] (1.2.840.10045.3.1.7)]
         * */
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(algorithm, provider);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        ECGenParameterSpec ecsp = new ECGenParameterSpec(nameParameterSpec);
        try {
            kpg.initialize(ecsp);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        KeyPair kp = kpg.genKeyPair();
        PrivateKey privKey = kp.getPrivate();
        PublicKey pubKey = kp.getPublic();

        if (pubKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) pubKey;
            /** Get x coordinate */
            byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);

            /** Get Y coordinate */
            byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);

            /** Get Curves params */
            String privHex = Hex.encodeHexString(privKey.getEncoded());
            log.info("\nCreating new RPK for the next start... \n" +
                    " Elliptic Curve parameters  : [{}] \n" +
                    " public_x :  [{}] \n" +
                    " public_y :  [{}] \n" +
                    " private_s : [{}] \n" +
                    " Public Key (Hex): [{}]\n" +
                    " Private Key (Hex): [{}]",
                    ecPublicKey.getParams().toString(),
                    Hex.encodeHexString(x),
                    Hex.encodeHexString(y),
                    privHex.substring(privHex.length() - 64),
                    Hex.encodeHexString(pubKey.getEncoded()),
                    Hex.encodeHexString(privKey.getEncoded()));
        }
    }
}

