// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import lombok.Data;

import java.io.Serializable;

@Data
public class RepositorySettings implements Serializable {
    private static final long serialVersionUID = -3211552851889198721L;

    private String repositoryUri;
    private RepositoryAuthMethod authMethod;
    private String username;
    private String password;
    private String privateKeyFileName;
    private String privateKey;
    private String privateKeyPassword;
    private String defaultBranch;
    private boolean readOnly;
    private boolean showMergeCommits;
    private boolean localOnly;

    public RepositorySettings() {
    }

    public RepositorySettings(RepositorySettings settings) {
        this.repositoryUri = settings.getRepositoryUri();
        this.authMethod = settings.getAuthMethod();
        this.username = settings.getUsername();
        this.password = settings.getPassword();
        this.privateKeyFileName = settings.getPrivateKeyFileName();
        this.privateKey = settings.getPrivateKey();
        this.privateKeyPassword = settings.getPrivateKeyPassword();
        this.defaultBranch = settings.getDefaultBranch();
        this.readOnly = settings.isReadOnly();
        this.showMergeCommits = settings.isShowMergeCommits();
        this.localOnly = settings.isLocalOnly();
    }

}
