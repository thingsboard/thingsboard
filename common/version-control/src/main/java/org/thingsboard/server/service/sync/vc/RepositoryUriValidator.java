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

import org.eclipse.jgit.transport.URIish;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

import java.net.URISyntaxException;
import java.util.Set;

public final class RepositoryUriValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "ssh");

    private RepositoryUriValidator() {
    }

    public static void validate(RepositorySettings settings) {
        if (settings == null || settings.isLocalOnly()) {
            return;
        }
        String uri = settings.getRepositoryUri();
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Repository URI is required");
        }
        URIish parsed;
        try {
            parsed = new URIish(uri.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Repository URI is invalid: " + e.getMessage(), e);
        }
        // URIish returns null scheme for scp-style git@host:path; treat as ssh
        String scheme = parsed.getScheme();
        String effectiveScheme = scheme == null ? "ssh" : scheme.toLowerCase();
        if (!ALLOWED_SCHEMES.contains(effectiveScheme)) {
            throw new IllegalArgumentException("Repository URI scheme '" + effectiveScheme + "' is not allowed. " +
                    "Allowed schemes: http, https, ssh");
        }
        String host = parsed.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Repository URI is missing a hostname");
        }
        // Validate the host extracted by URIish — the same parser JGit uses for transport.
        // Using java.net.URI here would risk parser-disagreement SSRF bypasses
        // (e.g. http://evil.com#@127.0.0.1/ where URI.getHost() and URIish.getHost() differ).
        try {
            SsrfProtectionValidator.validateHost(host);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Repository URI is not allowed: " + e.getMessage() +
                    ". If this host should be accessible, contact the platform administrator about SSRF allow-list.", e);
        }
    }
}
