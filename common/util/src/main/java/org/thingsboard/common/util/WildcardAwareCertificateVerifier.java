/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.common.util;

import org.apache.hc.client5.http.psl.PublicSuffixMatcher;
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerName;
import org.eclipse.californium.scandium.util.ServerNames;

import javax.net.ssl.SSLException;
import javax.security.auth.x500.X500Principal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * Scandium's own subject/hostname check ({@code DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT})
 * matches SAN entries with a plain {@code equalsIgnoreCase}, so it rejects wildcard
 * certificates (e.g. {@code *.example.com}) outright. This verifier delegates chain-of-trust
 * validation to {@link StaticNewAdvancedCertificateVerifier} (with its subject check disabled)
 * and delegates the actual hostname/IP identity check to Apache HttpClient's
 * {@link DefaultHostnameVerifier} (RFC 2818/6125 SAN + wildcard + IPv4/IPv6 matching), constructed
 * with a real {@link PublicSuffixMatcher} so its own built-in public-suffix rejection is active
 * (the no-arg constructor would otherwise silently disable it) instead of re-implementing
 * certificate-matching logic.
 * <p>
 * The library's public-suffix check only rejects a wildcard when its suffix is itself a bare
 * public suffix (e.g. {@code *.com}) — it doesn't reject multi-label public suffixes such as
 * {@code *.co.uk} or {@code *.github.io} (verified empirically; guarded by
 * {@code verifySubject_overlyBroadWildcard_returnsException} so a future httpclient5 upgrade that
 * changes this would fail the build rather than regress silently). {@link #matchesOverlyBroadWildcard}
 * closes that specific gap, scoped to only the SAN entry that would actually match the destination
 * (so an unrelated overly-broad SAN elsewhere on a multi-domain certificate can't cause a false
 * rejection).
 */
class WildcardAwareCertificateVerifier implements NewAdvancedCertificateVerifier {

    /** SAN {@code GeneralName} type for {@code dNSName}, see RFC 5280 4.2.1.6. */
    private static final int SAN_TYPE_DNS_NAME = 2;

    private static final PublicSuffixMatcher PUBLIC_SUFFIX_MATCHER = PublicSuffixMatcherLoader.getDefault();
    private static final DefaultHostnameVerifier HOSTNAME_VERIFIER = new DefaultHostnameVerifier(PUBLIC_SUFFIX_MATCHER);

    private final NewAdvancedCertificateVerifier delegate;

    WildcardAwareCertificateVerifier(X509Certificate[] trustedCertificates) {
        this.delegate = StaticNewAdvancedCertificateVerifier.builder()
                .setTrustedCertificates(trustedCertificates)
                .build();
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return delegate.getSupportedCertificateTypes();
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverNames, InetSocketAddress remotePeer,
            boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        CertificateVerificationResult result = delegate.verifyCertificate(cid, serverNames, remotePeer,
                clientUsage, false, truncateCertificatePath, message);
        if (!verifySubject || result.getException() != null || result.getCertificatePath() == null) {
            return result;
        }

        X509Certificate leafCertificate = (X509Certificate) result.getCertificatePath().getCertificates().get(0);
        HandshakeException mismatch = verifySubject(serverNames, remotePeer, leafCertificate);
        return mismatch == null ? result : new CertificateVerificationResult(cid, mismatch, null);
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        delegate.setResultHandler(resultHandler);
    }

    static HandshakeException verifySubject(ServerNames serverNames, InetSocketAddress remotePeer, X509Certificate certificate) {
        String destination = getDestination(serverNames, remotePeer);
        if (destination == null) {
            return null;
        }
        if (matchesOverlyBroadWildcard(certificate, destination)) {
            return mismatchException(certificate, "wildcard SAN covering destination '" + destination + "' spans a public suffix");
        }
        try {
            HOSTNAME_VERIFIER.verify(destination, certificate);
            return null;
        } catch (SSLException e) {
            return mismatchException(certificate, e.getMessage());
        }
    }

    /**
     * {@link DefaultHostnameVerifier} (even with a real {@link PublicSuffixMatcher}) only rejects
     * a wildcard when its suffix is itself a bare public suffix (e.g. {@code *.com}) — it doesn't
     * reject multi-label public suffixes such as {@code *.co.uk} or {@code *.github.io}. Only the
     * SAN entry that would actually be used to match {@code destination} is checked, so an
     * unrelated overly-broad wildcard elsewhere on a multi-domain certificate can't cause a false
     * rejection of an otherwise valid match.
     */
    private static boolean matchesOverlyBroadWildcard(X509Certificate certificate, String destination) {
        int firstDot = destination.indexOf('.');
        if (firstDot < 0) {
            return false;
        }
        String destinationSuffix = destination.substring(firstDot + 1);
        try {
            Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
            if (subjectAlternativeNames == null) {
                return false;
            }
            for (List<?> entry : subjectAlternativeNames) {
                if (isDnsNameEntry(entry)) {
                    String dnsName = (String) entry.get(1);
                    if (dnsName != null && dnsName.startsWith("*.")
                            && dnsName.substring(2).equalsIgnoreCase(destinationSuffix)
                            && PUBLIC_SUFFIX_MATCHER.matches(destinationSuffix)) {
                        return true;
                    }
                }
            }
        } catch (CertificateParsingException e) {
            return true; // fail closed: can't safely evaluate the SAN list
        }
        return false;
    }

    private static boolean isDnsNameEntry(List<?> subjectAlternativeNameEntry) {
        return subjectAlternativeNameEntry.size() >= 2 && Integer.valueOf(SAN_TYPE_DNS_NAME).equals(subjectAlternativeNameEntry.get(0));
    }

    private static HandshakeException mismatchException(X509Certificate certificate, String reason) {
        AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
        return new HandshakeException("Certificate " + CertPathUtil.getSubjectsCn(certificate) + ": " + reason, alert);
    }

    private static String getDestination(ServerNames serverNames, InetSocketAddress remotePeer) {
        String hostName = remotePeer != null ? StringUtil.toHostString(remotePeer) : null;
        String literalIp = null;
        if (remotePeer != null) {
            InetAddress address = remotePeer.getAddress();
            if (address != null) {
                literalIp = address.getHostAddress();
            }
        }
        if (serverNames != null) {
            ServerName serverName = serverNames.getServerName(ServerName.NameType.HOST_NAME);
            if (serverName != null) {
                hostName = serverName.getNameAsString();
            }
        }
        if (hostName != null && hostName.equals(literalIp)) {
            // no SNI hostname was presented, only a literal IP address to check
            hostName = null;
        }
        return hostName != null ? hostName : literalIp;
    }

}
