// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

class HostScopedCredentialsProvider extends UsernamePasswordCredentialsProvider {

    private final String allowedHost;

    HostScopedCredentialsProvider(String username, String password, String allowedHost) {
        super(username, password == null ? "" : password);
        this.allowedHost = allowedHost;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        if (uri == null || allowedHost == null) {
            return false;
        }
        String requestedHost = uri.getHost();
        if (requestedHost == null || !allowedHost.equalsIgnoreCase(requestedHost)) {
            return false;
        }
        return super.get(uri, items);
    }
}
