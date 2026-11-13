// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class HostScopedCredentialsProviderTest {

    @Test
    void sameHostReturnsCredentials() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", "pass", "github.com");
        URIish uri = new URIish("https://github.com/owner/repo.git");
        CredentialItem.Username userItem = new CredentialItem.Username();
        CredentialItem.Password passItem = new CredentialItem.Password();

        boolean result = provider.get(uri, userItem, passItem);

        assertThat(result).isTrue();
        assertThat(userItem.getValue()).isEqualTo("user");
        assertThat(new String(passItem.getValue())).isEqualTo("pass");
    }

    @Test
    void crossHostReturnsFalseAndDoesNotPopulateItems() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", "pass", "github.com");
        URIish redirectTarget = new URIish("https://attacker.example.com/owner/repo.git");
        CredentialItem.Username userItem = new CredentialItem.Username();
        CredentialItem.Password passItem = new CredentialItem.Password();

        boolean result = provider.get(redirectTarget, userItem, passItem);

        assertThat(result).isFalse();
        assertThat(userItem.getValue()).isNull();
        assertThat(passItem.getValue()).isNull();
    }

    @Test
    void hostComparisonIsCaseInsensitive() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", "pass", "GitHub.COM");
        URIish uri = new URIish("https://github.com/owner/repo.git");
        CredentialItem.Username userItem = new CredentialItem.Username();

        boolean result = provider.get(uri, userItem);

        assertThat(result).isTrue();
        assertThat(userItem.getValue()).isEqualTo("user");
    }

    @Test
    void crossHostToPrivateIpIsRefused() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", "pass", "github.com");
        URIish internalRedirect = new URIish("http://10.0.0.5/owner/repo.git");
        CredentialItem.Username userItem = new CredentialItem.Username();

        boolean result = provider.get(internalRedirect, userItem);

        assertThat(result).isFalse();
        assertThat(userItem.getValue()).isNull();
    }

    @Test
    void nullAllowedHostRefuses() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", "pass", null);
        URIish uri = new URIish("https://github.com/owner/repo.git");
        CredentialItem.Username userItem = new CredentialItem.Username();

        boolean result = provider.get(uri, userItem);

        assertThat(result).isFalse();
        assertThat(userItem.getValue()).isNull();
    }

    @Test
    void nullPasswordIsNormalizedToEmpty() throws URISyntaxException {
        HostScopedCredentialsProvider provider = new HostScopedCredentialsProvider("user", null, "github.com");
        URIish uri = new URIish("https://github.com/owner/repo.git");
        CredentialItem.Password passItem = new CredentialItem.Password();

        boolean result = provider.get(uri, passItem);

        assertThat(result).isTrue();
        assertThat(new String(passItem.getValue())).isEmpty();
    }
}
