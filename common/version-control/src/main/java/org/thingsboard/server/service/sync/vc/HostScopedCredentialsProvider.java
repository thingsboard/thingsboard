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
