/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.x509;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class TbAdvancedCertificateVerifier implements NewAdvancedCertificateVerifier {
    /**
     * Get the list of supported certificate types in order of preference.
     *
     * @return the list of supported certificate types.
     * @since 3.0 (renamed from getSupportedCertificateType)
     */
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        log.warn("client_getSupportedCertificateTypes");
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    /**
     * Validates the certificate provided by the the peer as part of the
     * certificate message.
     * <p>
     * If a x509 certificate chain is provided in the certificate message,
     * validate the chain and key usage. If a RawPublicKey certificate is
     * provided, check, if this public key is trusted.
     *
     * @param cid                     connection ID
     * @param serverName              indicated server names. May be {@code null}, if not
     *                                available or SNI is not enabled.
     * @param remotePeer              socket address of remote peer
     * @param clientUsage             indicator to check certificate usage. {@code true},
     *                                check key usage for client, {@code false} for server.
     * @param verifySubject           {@code true} to verify the certificate's subjects,
     *                                {@code false}, if not.
     * @param truncateCertificatePath {@code true} truncate certificate path at
     *                                a trusted certificate before validation.
     * @param message                 certificate message to be validated
     * @return certificate verification result, or {@code null}, if result is
     * provided asynchronous.
     * @since 3.0 (removed DTLSSession session, added remotePeer and
     * verifySubject)
     */
    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        log.warn("client_verifyCertificate");
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            return new CertificateVerificationResult(cid, certChain, null);
        }
    }

    /**
     * Return an list of certificate authorities which are trusted
     * for authenticating peers.
     *
     * @return a non-null (possibly empty) list of accepted CA issuers.
     */
    @Override
    public List<X500Principal> getAcceptedIssuers() {
        log.warn("getAcceptedIssuers");
        return null;
    }

    /**
     * Set the handler for asynchronous handshake results.
     * <p>
     * Called during initialization of the {@link DTLSConnector}. Synchronous
     * implementations may just ignore this using an empty implementation.
     *
     * @param resultHandler handler for asynchronous master secret results. This
     *                      handler MUST NOT be called from the thread calling
     *                      {@link #verifyCertificate(ConnectionId, ServerNames, InetSocketAddress, boolean, boolean, boolean, CertificateMessage)},
     *                      instead just return the result there.
     */
    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        log.warn("setResultHandler");
    }
}
