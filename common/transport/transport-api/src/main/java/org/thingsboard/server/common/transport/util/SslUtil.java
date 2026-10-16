// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.util;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.thingsboard.server.common.msg.EncryptionUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
public class SslUtil {

    private SslUtil() {
    }

    public static String getCertificateString(Certificate cert)
            throws CertificateEncodingException {
        return EncryptionUtil.certTrimNewLines(Base64.getEncoder().encodeToString(cert.getEncoded()));
    }

    public static String getCertificateChainString(Certificate[] chain)
            throws CertificateEncodingException {
        String begin = "-----BEGIN CERTIFICATE-----";
        String end = "-----END CERTIFICATE-----";
        StringBuilder stringBuilder = new StringBuilder();
        for (Certificate cert : chain) {
            stringBuilder.append(begin).append(EncryptionUtil.certTrimNewLines(Base64.getEncoder().encodeToString(cert.getEncoded()))).append(end).append("\n");
        }
        return stringBuilder.toString();
    }

    public static X509Certificate readCertFile(String fileContent) {
        X509Certificate certificate = null;
        try {
            if (fileContent != null && !fileContent.trim().isEmpty()) {
                fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----", "")
                        .replace("-----END CERTIFICATE-----", "")
                        .replaceAll("\\s", "");
                byte[] decoded = Base64.getDecoder().decode(fileContent);
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                try (InputStream inStream = new ByteArrayInputStream(decoded)) {
                    certificate = (X509Certificate) certFactory.generateCertificate(inStream);
                }
            }
        } catch (Exception ignored) {}
        return certificate;
    }

    public static String parseCommonName(X509Certificate certificate) {
        X500Name x500name;
        try {
            x500name = new JcaX509CertificateHolder(certificate).getSubject();
        } catch (CertificateEncodingException e) {
            log.warn("Cannot parse CN from device certificate");
            throw new RuntimeException(e);
        }
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

}
