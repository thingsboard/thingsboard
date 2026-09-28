// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.install;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.service.agent.upgrade.AgentUpgradeVersionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultAgentInstallInstructionsServiceTest {

    private static final String IMAGE = "thingsboard/tb-remote-agent:1.0.0";

    @Mock
    private AgentUpgradeVersionService agentUpgradeVersionService;
    @Mock
    private HttpServletRequest request;

    private DefaultAgentInstallInstructionsService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAgentInstallInstructionsService(agentUpgradeVersionService);
        ReflectionTestUtils.setField(service, "rpcPort", 7070);
        ReflectionTestUtils.setField(service, "sslEnabled", false);
        when(agentUpgradeVersionService.getLatestImageRef()).thenReturn(IMAGE);
        when(request.getServerName()).thenReturn("tb.example.com");
    }

    @Test
    void installInstructionsRenderEveryPlaceholder() {
        Agent agent = new Agent();
        agent.setRoutingKey("routing-key-value");
        agent.setSecret("routing-secret-value");

        AgentInstructions instructions = service.getInstallInstructions(agent, "docker", request);

        assertThat(instructions.getInstructions())
                .doesNotContain("${")
                .contains("-e TB_SERVER_ADDR=tb.example.com:7070")
                .contains("-e TB_RPC_SSL_ENABLED=false")
                .contains("-e TB_AGENT_ROUTING_KEY=routing-key-value")
                .contains("-e TB_AGENT_ROUTING_SECRET=routing-secret-value")
                .endsWith(IMAGE);
    }

    @Test
    void provisionInstructionsRenderEveryPlaceholder() {
        AgentProfile profile = new AgentProfile();
        profile.setProvisionKey("provision-key-value");
        profile.setProvisionSecret("provision-secret-value");

        AgentInstructions instructions = service.getProvisionInstructions(profile, "DOCKER", request);

        assertThat(instructions.getInstructions())
                .doesNotContain("${")
                .contains("-e TB_SERVER_ADDR=tb.example.com:7070")
                .contains("-e TB_RPC_SSL_ENABLED=false")
                .contains("-e AUTO_PROVISION=true")
                .contains("-e TB_PROVISION_KEY=provision-key-value")
                .contains("-e TB_PROVISION_SECRET=provision-secret-value")
                .endsWith(IMAGE);
    }

    @Test
    void localhostServerNameIsRewrittenForDocker() {
        when(request.getServerName()).thenReturn("localhost");
        Agent agent = new Agent();
        agent.setRoutingKey("k");
        agent.setSecret("s");

        assertThat(service.getInstallInstructions(agent, "docker", request).getInstructions())
                .startsWith("docker run -d \\\n  --add-host=host.docker.internal:host-gateway \\\n  --name=tb-agent \\\n")
                .contains("-e TB_SERVER_ADDR=host.docker.internal:7070");
    }

    @Test
    void localhostServerNameAddsDockerHostGatewayForProvision() {
        when(request.getServerName()).thenReturn("127.0.0.1");

        assertThat(service.getProvisionInstructions(new AgentProfile(), "docker", request).getInstructions())
                .startsWith("docker run -d \\\n  --add-host=host.docker.internal:host-gateway \\\n  --name=tb-agent \\\n")
                .contains("-e TB_SERVER_ADDR=host.docker.internal:7070");
    }

    @Test
    void remoteServerNameHasNoExtraHosts() {
        AgentInstructions instructions = service.getInstallInstructions(new Agent(), "docker", request);

        assertThat(instructions.getInstructions())
                .startsWith("docker run -d \\\n  --name=tb-agent \\\n")
                .doesNotContain("--add-host")
                .doesNotContain("${EXTRA_HOSTS}");
    }

    @Test
    void nullCredentialsRenderAsEmptyRatherThanLiteralNull() {
        AgentInstructions instructions = service.getInstallInstructions(new Agent(), "docker", request);

        assertThat(instructions.getInstructions())
                .contains("-e TB_AGENT_ROUTING_KEY= ")
                .doesNotContain("null");
    }

    @Test
    void sslEnabledIsPropagatedToInstallAndProvisionCommands() {
        ReflectionTestUtils.setField(service, "sslEnabled", true);

        assertThat(service.getInstallInstructions(new Agent(), "docker", request).getInstructions())
                .contains("-e TB_RPC_SSL_ENABLED=true");
        assertThat(service.getProvisionInstructions(new AgentProfile(), "docker", request).getInstructions())
                .contains("-e TB_RPC_SSL_ENABLED=true");
    }

    @Test
    void unsupportedMethodIsRejected() {
        assertThatThrownBy(() -> service.getInstallInstructions(new Agent(), "kubernetes", request))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getProvisionInstructions(new AgentProfile(), "kubernetes", request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
