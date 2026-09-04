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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ResourceLock("SsrfProtectionValidator") // shares static state with SsrfProtectionValidatorTest
class RepositoryUriValidatorTest {

    @AfterEach
    void reset() {
        SsrfProtectionValidator.setEnabled(false);
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
    }

    private static RepositorySettings settings(String uri) {
        RepositorySettings s = new RepositorySettings();
        s.setRepositoryUri(uri);
        return s;
    }

    private static RepositorySettings localOnly(String uri) {
        RepositorySettings s = settings(uri);
        s.setLocalOnly(true);
        return s;
    }

    // --- localOnly skip ---

    @Test
    void localOnlySkipsAllValidation() {
        // Even file:// and unresolvable hosts pass when localOnly=true
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(localOnly("file:///etc/passwd")));
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(localOnly("anything-not-a-uri")));
    }

    @Test
    void nullSettingsIsNoOp() {
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(null));
    }

    // --- null / blank URI ---

    @Test
    void nullUriIsRejected() {
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository URI is required");
    }

    @Test
    void blankUriIsRejected() {
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository URI is required");
    }

    // --- scheme whitelist ---

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///tmp/repo",
            "git://github.com/foo/bar",
            "ftp://server/repo"
    })
    void disallowedSchemesAreRejected(String uri) {
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings(uri)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com/foo.git",
            "https://example.com/foo.git",
            "ssh://git@example.com/foo.git",
            "git@github.com:owner/repo.git" // scp-style → SSH inferred
    })
    void allowedSchemesPassWhenSsrfDisabled(String uri) {
        SsrfProtectionValidator.setEnabled(false);
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings(uri)));
    }

    // --- host checks ---

    @Test
    void emptyHostIsRejected() {
        // http:/// (no host)
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("https:///path")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hostname");
    }

    // --- SSRF integration: HTTP/HTTPS ---

    @Test
    void httpPrivateIpRejectedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("https://10.0.0.5/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void httpLocalhostRejectedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("http://localhost/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void httpPublicIpAllowedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("https://8.8.8.8/foo.git")));
    }

    // --- SSRF integration: SSH (uses validateHost) ---

    @Test
    void sshPrivateIpRejectedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("ssh://git@192.168.1.10/foo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void scpStyleSshPrivateIpRejectedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("git@10.0.0.1:owner/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void sshInternalSuffixRejectedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("ssh://git@server.internal/foo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void sshPublicHostAllowedWhenSsrfEnabled() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("ssh://git@github.com/foo/bar.git")));
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("git@github.com:foo/bar.git")));
    }

    // --- allow-list interaction ---

    @Test
    void sshHostInAllowListPassesEvenWhenPrivate() {
        SsrfProtectionValidator.setEnabled(true);
        SsrfProtectionValidator.setAllowedHosts(List.of("10.0.0.0/8"));
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("ssh://git@10.1.2.3/foo.git")));
    }

    @Test
    void httpHostInAllowListPassesEvenWhenPrivate() {
        SsrfProtectionValidator.setEnabled(true);
        SsrfProtectionValidator.setAllowedHosts(List.of("internal-git.corp"));
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("https://internal-git.corp/foo.git")));
    }

    // --- Parser-consistency: host must come from URIish (the parser JGit transport uses) ---

    @Test
    void userInfoBeforeInternalHostIsRejected() {
        // http://github.com@127.0.0.1/ — URIish parses userinfo=github.com, host=127.0.0.1.
        // JGit transport will connect to 127.0.0.1. Validator must see the same host.
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("http://github.com@127.0.0.1/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void userInfoBeforePrivateCidrHostIsRejected() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("https://fake-user:fake-pass@10.0.0.5/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void userInfoBeforePublicHostIsAllowed() {
        // The userinfo is just credentials — host is what matters.
        SsrfProtectionValidator.setEnabled(true);
        assertThatNoException().isThrownBy(() -> RepositoryUriValidator.validate(settings("https://user:pass@github.com/repo.git")));
    }

    @Test
    void ipv6LoopbackLiteralIsRejected() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> RepositoryUriValidator.validate(settings("https://[::1]/repo.git")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }
}
