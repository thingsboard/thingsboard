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
package org.thingsboard.server.common.transport.config.ssl;

import org.thingsboard.server.common.data.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSslCredentials implements SslCredentials {

    private record SslState(
            char[] keyPasswordArray,
            KeyStore keyStore,
            PrivateKey privateKey,
            PublicKey publicKey,
            X509Certificate[] chain,
            X509Certificate[] trusts
    ) {}

    private final AtomicReference<SslState> state = new AtomicReference<>();

    @Override
    public void init(boolean trustsOnly) throws IOException, GeneralSecurityException {
        SslState newState = buildState(trustsOnly);
        state.set(newState);
    }

    @Override
    public void reload(boolean trustsOnly) throws IOException, GeneralSecurityException {
        init(trustsOnly);
    }

    private SslState buildState(boolean trustsOnly) throws IOException, GeneralSecurityException {
        String keyPassword = getKeyPassword();
        char[] keyPasswordArray;
        if (StringUtils.isEmpty(keyPassword)) {
            keyPasswordArray = new char[0];
        } else {
            keyPasswordArray = keyPassword.toCharArray();
        }
        KeyStore keyStore = this.loadKeyStore(trustsOnly, keyPasswordArray);
        Set<X509Certificate> trustedCerts = getTrustedCerts(keyStore, trustsOnly);
        X509Certificate[] trusts = trustedCerts.toArray(new X509Certificate[0]);
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        X509Certificate[] chain = null;
        if (!trustsOnly) {
            PrivateKeyEntry privateKeyEntry = null;
            String keyAlias = this.getKeyAlias();
            if (!StringUtils.isEmpty(keyAlias)) {
                privateKeyEntry = tryGetPrivateKeyEntry(keyStore, keyAlias, keyPasswordArray);
            } else {
                for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    privateKeyEntry = tryGetPrivateKeyEntry(keyStore, alias, keyPasswordArray);
                    if (privateKeyEntry != null) {
                        this.updateKeyAlias(alias);
                        break;
                    }
                }
            }
            if (privateKeyEntry == null) {
                throw new IllegalArgumentException("Failed to get private key from the keystore or pem files. " +
                        "Please check if the private key exists in the keystore or pem files and if the provided private key password is valid.");
            }
            chain = asX509Certificates(privateKeyEntry.getCertificateChain());
            privateKey = privateKeyEntry.getPrivateKey();
            if (chain.length > 0) {
                publicKey = chain[0].getPublicKey();
            }
        }
        return new SslState(keyPasswordArray, keyStore, privateKey, publicKey, chain, trusts);
    }

    private SslState getState() {
        SslState s = state.get();
        if (s == null) {
            throw new IllegalStateException("SSL credentials not initialized. Call init() first.");
        }
        return s;
    }

    @Override
    public KeyStore getKeyStore() {
        return getState().keyStore;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return getState().privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return getState().publicKey;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        return getState().chain;
    }

    @Override
    public X509Certificate[] getTrustedCertificates() {
        return getState().trusts;
    }

    @Override
    public TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        SslState s = getState();
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(s.keyStore);
        return tmFactory;
    }

    @Override
    public KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        SslState s = getState();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(s.keyStore, s.keyPasswordArray);
        return kmf;
    }

    @Override
    public String getValueFromSubjectNameByKey(String subjectName, String key) {
        String[] dns = subjectName.split(",");
        Optional<String> cn = (Arrays.stream(dns).filter(dn -> dn.contains(key + "="))).findFirst();
        String value = cn.map(s -> s.replace(key + "=", "")).orElse(null);
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    protected abstract boolean canUse();

    protected abstract KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray) throws IOException, GeneralSecurityException;

    protected abstract void updateKeyAlias(String keyAlias);

    private static X509Certificate[] asX509Certificates(Certificate[] certificates) {
        if (null == certificates || 0 == certificates.length) {
            throw new IllegalArgumentException("certificates missing!");
        }
        X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
        for (int index = 0; certificates.length > index; ++index) {
            if (null == certificates[index]) {
                throw new IllegalArgumentException("[" + index + "] is null!");
            }
            try {
                x509Certificates[index] = (X509Certificate) certificates[index];
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("[" + index + "] is not a x509 certificate! Instead it's a "
                        + certificates[index].getClass().getName());
            }
        }
        return x509Certificates;
    }

    private static PrivateKeyEntry tryGetPrivateKeyEntry(KeyStore keyStore, String alias, char[] pwd) {
        PrivateKeyEntry entry = null;
        try {
            if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                try {
                    entry = (KeyStore.PrivateKeyEntry) keyStore
                            .getEntry(alias, new KeyStore.PasswordProtection(pwd));
                } catch (UnsupportedOperationException e) {
                    PrivateKey key = (PrivateKey) keyStore.getKey(alias, pwd);
                    Certificate[] certs = keyStore.getCertificateChain(alias);
                    entry = new KeyStore.PrivateKeyEntry(key, certs);
                }
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException ignored) {}
        return entry;
    }

    private static Set<X509Certificate> getTrustedCerts(KeyStore ks, boolean trustsOnly) {
        Set<X509Certificate> set = new HashSet<>();
        try {
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                if (ks.isCertificateEntry(alias)) {
                    Certificate cert = ks.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        if (trustsOnly) {
                            // is CA certificate
                            if (((X509Certificate) cert).getBasicConstraints() >= 0) {
                                set.add((X509Certificate) cert);
                            }
                        } else {
                            set.add((X509Certificate) cert);
                        }
                    }
                } else if (ks.isKeyEntry(alias)) {
                    Certificate[] certs = ks.getCertificateChain(alias);
                    if ((certs != null) && (certs.length > 0) &&
                            (certs[0] instanceof X509Certificate)) {
                        if (trustsOnly) {
                            for (Certificate cert : certs) {
                                // is CA certificate
                                if (((X509Certificate) cert).getBasicConstraints() >= 0) {
                                    set.add((X509Certificate) cert);
                                }
                            }
                        } else {
                            set.add((X509Certificate) certs[0]);
                        }
                    }
                }
            }
        } catch (KeyStoreException ignored) {}
        return Collections.unmodifiableSet(set);
    }

}
