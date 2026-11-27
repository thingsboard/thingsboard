// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.model.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The isolation the {@code extensions.docker.network.internal} flag promises is enforced by Docker at
 * network CREATE time and can never be turned on afterwards. These cover what the provider does when the
 * network already exists — the default setup, since auto-create is off.
 */
class ExtensionDockerClientProviderTest {

    private static final String NETWORK = "tb-extensions";

    private final DockerClient client = mock(DockerClient.class);

    private void networkExists(Boolean internal) {
        Network network = mock(Network.class);
        when(network.getName()).thenReturn(NETWORK);
        when(network.getInternal()).thenReturn(internal);
        listReturns(List.of(network));
    }

    private void networkMissing() {
        listReturns(List.of());
    }

    private void listReturns(List<Network> networks) {
        ListNetworksCmd cmd = mock(ListNetworksCmd.class, RETURNS_SELF);
        when(client.listNetworksCmd()).thenReturn(cmd);
        when(cmd.exec()).thenReturn(networks);
    }

    private ExtensionDockerClientProvider provider(boolean autoCreate, boolean internal) {
        return new ExtensionDockerClientProvider(client, NETWORK, autoCreate, internal);
    }

    // The whole point of the flag is that tenant containers cannot reach the host or 169.254.169.254.
    // Silently running without it — which is what a plain existence check did — is the failure mode worth
    // a refused startup: the operator believes egress is closed and it is wide open.
    @Test
    void startupFailsWhenTheExistingNetworkDoesNotEnforceTheRequestedIsolation() {
        networkExists(false);

        assertThatThrownBy(() -> provider(false, true).ensureNetwork())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is NOT internal");
    }

    @Test
    void anExistingInternalNetworkSatisfiesTheRequest() {
        networkExists(true);
        ExtensionDockerClientProvider provider = provider(false, true);

        provider.ensureNetwork();

        assertThat(provider.isEffectiveInternalNetwork()).isTrue();
    }

    // internal=false asks for nothing, so an existing plain bridge is fine — but what egress it allows is
    // now read off the live network rather than off the requested flag.
    @Test
    void anExistingPlainNetworkIsAcceptedAndReportedAsNotInternal() {
        networkExists(false);
        ExtensionDockerClientProvider provider = provider(false, false);

        provider.ensureNetwork();

        assertThat(provider.isEffectiveInternalNetwork()).isFalse();
    }

    @Test
    void aNetworkTbCreatesItselfCarriesTheRequestedIsolation() {
        networkMissing();
        CreateNetworkCmd createCmd = mock(CreateNetworkCmd.class, RETURNS_SELF);
        when(client.createNetworkCmd()).thenReturn(createCmd);
        ExtensionDockerClientProvider provider = provider(true, true);

        provider.ensureNetwork();

        verify(createCmd).withInternal(true);
        assertThat(provider.isEffectiveInternalNetwork()).isTrue();
    }

    // Nothing exists yet and TB will not create it, so whatever the operator makes by hand is unknown to
    // us: assume the weaker setting so the egress warning is printed rather than suppressed.
    @Test
    void aNetworkTbWillNotCreateIsAssumedNotInternal() {
        networkMissing();
        ExtensionDockerClientProvider provider = provider(false, true);

        provider.ensureNetwork();

        assertThat(provider.isEffectiveInternalNetwork()).isFalse();
    }

}
