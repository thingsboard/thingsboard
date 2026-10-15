// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.config.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class KeystoreSslCredentials extends AbstractSslCredentials {

    private String type;
    private String storeFile;
    private String storePassword;
    private String keyPassword;
    private String keyAlias;

    @Override
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.storeFile);
    }

    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        String keyStoreType = StringUtils.isEmpty(this.type) ? KeyStore.getDefaultType() : this.type;
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream tsFileInputStream = ResourceUtils.getInputStream(this, this.storeFile)) {
            keyStore.load(tsFileInputStream, StringUtils.isEmpty(this.storePassword) ? new char[0] : this.storePassword.toCharArray());
        }
        return keyStore;
    }

    @Override
    protected void updateKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    @Override
    public List<Path> getCertificateFilePaths() {
        if (!StringUtils.isEmpty(storeFile) && !storeFile.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            // Include the path even if the file doesn't exist yet — the watcher uses mtime=0 / checksum="" as
            // baseline, so a late-appearing file (e.g., mounted after boot) will be detected and trigger a reload.
            return Collections.singletonList(Path.of(storeFile).toAbsolutePath());
        }
        return Collections.emptyList();
    }

}
