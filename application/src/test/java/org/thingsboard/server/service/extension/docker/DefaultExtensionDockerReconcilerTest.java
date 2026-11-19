// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RestartContainerCmd;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.RestartPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;
import org.thingsboard.server.service.extension.spi.ExtensionSpecHasher;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behavioural coverage for {@link DefaultExtensionDockerReconciler} against a mocked {@link DockerClient}:
 * create-when-absent, idempotent no-op, recreate-on-spec-change, stop/restart/delete, the orphan-sweep
 * listing, the container-state → {@link DeploymentHealth} mapping, and the retry classification.
 */
class DefaultExtensionDockerReconcilerTest {

    private static final String SLUG = "acme-billing-9f3a";
    private static final String NAME = ExtensionResourceNames.container(SLUG);
    private static final String NETWORK = "tb-extensions";

    private DockerClient client;
    private DefaultExtensionDockerReconciler reconciler;
    private ExtensionContainerPlanner planner;

    private InspectContainerCmd inspectContainerCmd;
    private CreateContainerCmd createContainerCmd;
    private StartContainerCmd startContainerCmd;
    private StopContainerCmd stopContainerCmd;
    private RestartContainerCmd restartContainerCmd;
    private RemoveContainerCmd removeContainerCmd;
    private ListContainersCmd listContainersCmd;
    private PullImageCmd pullCmd;

    @BeforeEach
    void setUp() {
        client = mock(DockerClient.class);
        planner = new ExtensionContainerPlanner("http://tb-core:8080", "/api/extension/route");
        reconciler = new DefaultExtensionDockerReconciler(client, NETWORK, planner, 10, 1000L, 1000L);

        inspectContainerCmd = mock(InspectContainerCmd.class);
        when(client.inspectContainerCmd(NAME)).thenReturn(inspectContainerCmd);

        createContainerCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        when(client.createContainerCmd(anyString())).thenReturn(createContainerCmd);

        startContainerCmd = mock(StartContainerCmd.class, RETURNS_SELF);
        when(client.startContainerCmd(NAME)).thenReturn(startContainerCmd);

        stopContainerCmd = mock(StopContainerCmd.class, RETURNS_SELF);
        when(client.stopContainerCmd(NAME)).thenReturn(stopContainerCmd);

        restartContainerCmd = mock(RestartContainerCmd.class, RETURNS_SELF);
        when(client.restartContainerCmd(NAME)).thenReturn(restartContainerCmd);

        removeContainerCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);
        when(client.removeContainerCmd(NAME)).thenReturn(removeContainerCmd);

        listContainersCmd = mock(ListContainersCmd.class, RETURNS_SELF);
        when(client.listContainersCmd()).thenReturn(listContainersCmd);

        // Image present locally: with always-pull on this no longer short-circuits the pull, but the
        // recreate paths still need the inspect to succeed.
        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(client.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);

        pullCmd = mock(PullImageCmd.class, RETURNS_SELF);
        when(client.pullImageCmd(anyString())).thenReturn(pullCmd);
        pullSucceeds();
    }

    // The reconciler streams the pull into a PullImageResultCallback, which decides success from the LAST
    // item it saw — the daemon reports an error that happens after the first progress bytes as an in-band
    // item on an otherwise-200 stream, not as an HTTP error. exec(callback) returns the passed callback;
    // feed it a terminal item and complete it so awaitCompletion returns at once.
    private void pullEmits(PullResponseItem item) {
        // doAnswer, not when(...): when() would call exec(null) on the mock and run the previous answer.
        doAnswer(inv -> {
            ResultCallback<PullResponseItem> cb = inv.getArgument(0);
            cb.onNext(item);
            cb.onComplete();
            return cb;
        }).when(pullCmd).exec(any());
    }

    private void pullSucceeds() {
        PullResponseItem ok = mock(PullResponseItem.class);
        when(ok.getStatus()).thenReturn("Status: Downloaded newer image for registry.example.com/acme/billing-ext:1.4.0");
        when(ok.isPullSuccessIndicated()).thenReturn(true);
        pullEmits(ok);
    }

    private void pullFailsMidStream() {
        PullResponseItem failed = mock(PullResponseItem.class);
        when(failed.getStatus()).thenReturn("Pulling fs layer");
        when(failed.isPullSuccessIndicated()).thenReturn(false);
        pullEmits(failed);
    }

    private Extension extension(ExtensionState state) {
        Extension e = new Extension(new ExtensionId(UUID.randomUUID()));
        e.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        e.setName("Acme Billing");
        e.setSlug(SLUG);
        e.setState(state);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag("1.4.0");
        spec.setPort(9000);
        e.setContainerSpec(spec);
        return e;
    }

    private void containerAbsent() {
        when(inspectContainerCmd.exec()).thenThrow(new NotFoundException("absent"));
    }

    private void containerRunningHealthy(Extension ext) {
        containerPresent(ext, specHash(ext), true, "healthy", "running", null);
    }

    // The reconciler folds its own render context (planner context + network + host-config caps) into the
    // hash; seed stored labels the same way. Mirrors DefaultExtensionDockerReconciler.renderContext().
    private String specHash(Extension ext) {
        // always-pull is deliberately NOT part of the context: flipping it must not recreate containers
        String context = String.join("\n", planner.renderContext(), NETWORK, "256", "1024", "10m", "3");
        return ExtensionSpecHasher.hash(ext, null, context);
    }

    // Owner-less variant: managed but no owner labels — a previous slug owner's leftover container.
    // Ownership-checking paths must treat it as foreign; only deleteBySlug still acts on it.
    private void containerPresent(String specHash, boolean running, String healthStatus, String status, Long exitCode) {
        containerPresent(null, specHash, running, healthStatus, status, exitCode);
    }

    private void containerPresent(Extension owner, String specHash, boolean running, String healthStatus, String status, Long exitCode) {
        InspectContainerResponse resp = mock(InspectContainerResponse.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getLabels()).thenReturn(containerLabels(owner, specHash));
        when(resp.getConfig()).thenReturn(config);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(state.getRunning()).thenReturn(running);
        when(state.getStatus()).thenReturn(status);
        when(state.getExitCodeLong()).thenReturn(exitCode);
        if (healthStatus != null) {
            HealthState health = mock(HealthState.class);
            when(health.getStatus()).thenReturn(healthStatus);
            when(state.getHealth()).thenReturn(health);
        }
        when(resp.getState()).thenReturn(state);
        when(resp.getRestartCount()).thenReturn(0);
        when(inspectContainerCmd.exec()).thenReturn(resp);
    }

    @Test
    void applyCreatesAndStartsWhenAbsent() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();

        reconciler.apply(ext);

        verify(client).createContainerCmd("registry.example.com/acme/billing-ext:1.4.0");
        verify(createContainerCmd).withName(NAME);
        verify(createContainerCmd).exec();
        verify(client).startContainerCmd(NAME);
        verify(startContainerCmd).exec();
        verify(client, never()).removeContainerCmd(anyString());
    }

    // An in-stream pull failure used to be swallowed: apply() went on to recreate the container from
    // whatever stale copy of repository:tag the host had cached and stamped the NEW spec hash on it, so
    // every later apply/resync was a no-op and the tenant saw ACTIVE while the old code kept serving.
    @Test
    void applyFailsWhenPullReportsAnErrorInsideTheStream() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();
        pullFailsMidStream();

        assertThatThrownBy(() -> reconciler.apply(ext)).isInstanceOf(RuntimeException.class);

        verify(client, never()).createContainerCmd(anyString());
    }

    // Every managed container gets the full hardening set. RETURNS_SELF on createContainerCmd swallows a
    // dropped .withCapDrop(...) / .withNetworkMode(...) silently, so assert the HostConfig itself: a
    // refactor that loses one of these must fail CI, not ship.
    @Test
    void createdContainerCarriesTheFullHostConfigHardening() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        ext.getContainerSpec().setResources(null); // default tier
        containerAbsent();

        reconciler.apply(ext);

        ArgumentCaptor<HostConfig> captor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createContainerCmd).withHostConfig(captor.capture());
        HostConfig hostConfig = captor.getValue();

        assertThat(hostConfig.getNetworkMode()).isEqualTo(NETWORK);
        assertThat(hostConfig.getCapDrop()).containsExactly(Capability.ALL);
        assertThat(hostConfig.getReadonlyRootfs()).isTrue();
        assertThat(hostConfig.getSecurityOpts()).contains("no-new-privileges:true");
        assertThat(hostConfig.getTmpFs()).containsEntry("/tmp", "rw,size=64m");
        assertThat(hostConfig.getRestartPolicy()).isEqualTo(RestartPolicy.unlessStoppedRestart());
        // one tenant must not be able to exhaust the host's pids or file descriptors
        assertThat(hostConfig.getPidsLimit()).isEqualTo(256L);
        assertThat(hostConfig.getUlimits()).isNotEmpty();
        // no rotation = one chatty container fills the disk shared with tb-core and the database
        assertThat(hostConfig.getLogConfig()).isNotNull();
        assertThat(hostConfig.getLogConfig().getConfig()).containsEntry("max-size", "10m");
        // swap pinned to the memory limit, otherwise the container may commit its limit again in swap
        assertThat(hostConfig.getMemory()).isPositive();
        assertThat(hostConfig.getMemorySwap()).isEqualTo(hostConfig.getMemory());
        assertThat(hostConfig.getNanoCPUs()).isPositive();
    }

    // A VOLUME line in the tenant image makes Docker back that path with an anonymous read-write volume
    // on the host disk — uncapped, and outside the read-only rootfs. Every declared path must instead get
    // the same size-capped tmpfs as /tmp (a tmpfs mount at the path wins over the image's VOLUME).
    @Test
    void imageDeclaredVolumesAreReplacedByCappedTmpfs() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();
        InspectImageResponse image = mock(InspectImageResponse.class);
        ContainerConfig config = mock(ContainerConfig.class);
        Map<String, Object> volumes = new java.util.LinkedHashMap<>();
        volumes.put("/var/lib/app-data", Map.of());
        volumes.put("/cache", Map.of());
        doReturn(volumes).when(config).getVolumes();
        when(image.getConfig()).thenReturn(config);
        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(inspectImageCmd.exec()).thenReturn(image);
        when(client.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);

        reconciler.apply(ext);

        ArgumentCaptor<HostConfig> captor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createContainerCmd).withHostConfig(captor.capture());
        assertThat(captor.getValue().getTmpFs())
                .containsEntry("/tmp", "rw,size=64m")
                .containsEntry("/var/lib/app-data", "rw,size=64m")
                .containsEntry("/cache", "rw,size=64m");
    }

    // Fail CLOSED: if the image cannot be inspected the paths that need a cap are unknown, and a container
    // created anyway would run with exactly the uncapped host volumes G6 exists to prevent — stamped with
    // the desired hash, so no resync would ever repair it. The apply must fail instead (and be retried).
    @Test
    void applyFailsInsteadOfCreatingAContainerWhenTheImageCannotBeInspected() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();
        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(inspectImageCmd.exec()).thenThrow(new DockerException("daemon hiccup", 500));
        when(client.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);

        assertThatThrownBy(() -> reconciler.apply(ext)).isInstanceOf(DockerException.class);

        verify(client, never()).createContainerCmd(anyString());
        verify(client, never()).startContainerCmd(anyString());
    }

    // Same-owner recreate: the image is inspected BEFORE the old container is removed, so an inspection
    // failure leaves the healthy old container serving rather than removing it and then failing.
    @Test
    void inspectionFailureOnRecreateLeavesTheOldContainerInPlace() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerRunningHealthy(ext);
        ext.getContainerSpec().setTag("1.5.0"); // spec changed -> recreate path
        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(inspectImageCmd.exec()).thenThrow(new DockerException("daemon hiccup", 500));
        when(client.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);

        assertThatThrownBy(() -> reconciler.apply(ext)).isInstanceOf(DockerException.class);

        verify(client, never()).removeContainerCmd(anyString());
        verify(client, never()).createContainerCmd(anyString());
    }

    @Test
    void applyIsNoOpWhenHashUnchangedAndRunning() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerRunningHealthy(ext);

        reconciler.apply(ext);

        verify(client, never()).createContainerCmd(anyString());
        verify(client, never()).removeContainerCmd(anyString());
        verify(client, never()).startContainerCmd(anyString());
    }

    @Test
    void applyRecreatesWhenSpecChanged() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerPresent("STALE-HASH", true, "healthy", "running", null);

        reconciler.apply(ext);

        verify(client).removeContainerCmd(NAME);
        verify(removeContainerCmd).withForce(true);
        // 'docker rm -v': an image-declared VOLUME otherwise leaves an uncapped host-backed volume behind
        // on every single recreate, bypassing both the read-only rootfs and the 64m /tmp tmpfs cap.
        verify(removeContainerCmd).withRemoveVolumes(true);
        verify(client).createContainerCmd("registry.example.com/acme/billing-ext:1.4.0");
        verify(client).startContainerCmd(NAME);
    }

    // The container name is the DNS name serving this slug's traffic, so on the foreign-owner
    // recreate path the removal must come BEFORE the pull — pull-first would keep it serving.
    @Test
    void applyRemovesForeignOwnerContainerEvenWhenThePullFails() {
        Extension ext = extension(ExtensionState.ACTIVE);
        // same spec hash, but no owner labels: a leftover container of the slug's previous owner
        containerPresent(specHash(ext), true, "healthy", "running", null);
        pullFailsMidStream();

        assertThatThrownBy(() -> reconciler.apply(ext)).isInstanceOf(RuntimeException.class);

        verify(client).removeContainerCmd(NAME);
        verify(removeContainerCmd).exec();
        verify(client, never()).createContainerCmd(anyString());
    }

    @Test
    void applyStartsWhenHashUnchangedButStopped() {
        Extension ext = extension(ExtensionState.ACTIVE);
        // running=false but spec is current: converge by starting, not recreating
        containerPresent(ext, specHash(ext), false, null, "created", null);

        reconciler.apply(ext);

        verify(client, never()).removeContainerCmd(anyString());
        verify(client, never()).createContainerCmd(anyString());
        verify(client).startContainerCmd(NAME);
    }

    @Test
    void stoppedExtensionApplyDoesNotStart() {
        Extension ext = extension(ExtensionState.STOPPED);
        containerAbsent();

        reconciler.apply(ext);

        verify(client).createContainerCmd(anyString());
        verify(client, never()).startContainerCmd(anyString());
    }

    @Test
    void scaleToZeroStopsRunningContainer() {
        Extension ext = extension(ExtensionState.STOPPED);
        containerRunningHealthy(ext);

        reconciler.scaleTo(ext, 0);

        verify(client).stopContainerCmd(NAME);
        verify(stopContainerCmd).withTimeout(10);
        verify(stopContainerCmd).exec();
    }

    @Test
    void scaleToZeroIsNoOpWhenAbsent() {
        Extension ext = extension(ExtensionState.STOPPED);
        containerAbsent();

        reconciler.scaleTo(ext, 0);

        verify(client, never()).stopContainerCmd(anyString());
    }

    @Test
    void restartBouncesExistingContainer() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerRunningHealthy(ext);

        reconciler.restart(ext);

        verify(client).restartContainerCmd(NAME);
        verify(restartContainerCmd).withTimeout(10);
        verify(restartContainerCmd).exec();
    }

    @Test
    void restartSkippedWhenAbsent() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerAbsent();

        reconciler.restart(ext);

        verify(client, never()).restartContainerCmd(anyString());
    }

    // scale/restart must treat a foreign-owner container (previous slug holder, possibly another
    // tenant's) as absent, like the read paths do.
    @Test
    void scaleTreatsForeignOwnerContainerAsAbsent() {
        containerPresent("some-hash", true, "healthy", "running", null); // managed, but not ours

        reconciler.scaleTo(extension(ExtensionState.STOPPED), 0);

        verify(client, never()).stopContainerCmd(anyString());
        verify(client, never()).startContainerCmd(anyString());
    }

    @Test
    void restartTreatsForeignOwnerContainerAsAbsent() {
        containerPresent("some-hash", true, "healthy", "running", null);

        reconciler.restart(extension(ExtensionState.ACTIVE));

        verify(client, never()).restartContainerCmd(anyString());
    }

    @Test
    void deleteBySlugForceRemovesContainer() {
        containerPresent("some-hash", true, "healthy", "running", null);

        reconciler.deleteBySlug(SLUG);

        verify(client).removeContainerCmd(NAME);
        verify(removeContainerCmd).withForce(true);
        verify(removeContainerCmd).exec();
    }

    @Test
    void deleteBySlugToleratesMissingContainer() {
        containerAbsent();
        // must not propagate — teardown is idempotent
        reconciler.deleteBySlug(SLUG);
        verify(client, never()).removeContainerCmd(anyString());
    }

    // The ownership check and the removal are separate calls, so the container can still disappear
    // between them; that race must stay tolerated rather than failing teardown.
    @Test
    void deleteBySlugToleratesContainerVanishingAfterTheOwnershipCheck() {
        containerPresent("some-hash", true, "healthy", "running", null);
        when(removeContainerCmd.exec()).thenThrow(new NotFoundException("gone"));

        reconciler.deleteBySlug(SLUG);

        verify(removeContainerCmd).exec();
    }

    @Test
    void listManagedSlugsReadsSlugLabel() {
        Container a = mock(Container.class);
        when(a.getLabels()).thenReturn(Map.of(ExtensionResourceNames.LABEL_EXTENSION_SLUG, SLUG));
        Container b = mock(Container.class);
        when(b.getLabels()).thenReturn(Map.of(ExtensionResourceNames.LABEL_EXTENSION_SLUG, "other-1234"));
        when(listContainersCmd.exec()).thenReturn(List.of(a, b));

        assertThat(reconciler.listManagedSlugs()).containsExactlyInAnyOrder(SLUG, "other-1234");
    }

    @Test
    void describeMissingContainer() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.exists()).isFalse();
    }

    @Test
    void describeRunningHealthyIsReady() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerRunningHealthy(ext);

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.exists()).isTrue();
        assertThat(health.desiredReplicas()).isEqualTo(1);
        assertThat(health.readyReplicas()).isEqualTo(1);
        assertThat(health.errorReason()).isNull();
    }

    @Test
    void describeRunningButStartingIsNotReady() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerPresent(ext, specHash(ext), true, "starting", "running", null);

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.readyReplicas()).isZero();
        assertThat(health.errorReason()).isNull();
    }

    @Test
    void describeExitedSurfacesErrorReason() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerPresent(ext, specHash(ext), false, null, "exited", 1L);

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.exists()).isTrue();
        assertThat(health.readyReplicas()).isZero();
        assertThat(health.errorReason()).isEqualTo("Exited(1)");
    }

    // The daemon's raw State.Error embeds host paths, overlay2 ids and runc internals, and this reason
    // flows straight into the tenant-facing status and the unbounded last_error column. Every other
    // branch here returns a short constant and the K8s backend allow-lists three reason codes; the
    // fall-through must not be the one place that leaks host internals to a tenant.
    @Test
    void describeDoesNotSurfaceTheRawDaemonErrorString() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        String raw = "failed to create shim task: OCI runtime create failed: "
                + "/var/lib/docker/overlay2/9f3a1c/merged: no such device";
        containerWithDaemonError(ext, specHash(ext), raw);

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.errorReason()).isEqualTo("ContainerError");
        assertThat(health.errorReason()).doesNotContain("/var/lib/docker", "overlay2", "OCI runtime");
    }

    // The requested line count bounds nothing on its own: the tenant owns the container, so it owns how
    // long each line is. Without a byte budget one authenticated request materializes the whole log on
    // the core's heap, repeatable at will.
    @Test
    void tailLogsIsCappedByAByteBudget() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerPresent(ext, specHash(ext), true, null, "running", null);
        LogContainerCmd logCmd = mock(LogContainerCmd.class, RETURNS_SELF);
        when(client.logContainerCmd(anyString())).thenReturn(logCmd);
        // one oversized frame, as a container emitting very long lines would produce
        byte[] huge = "x".repeat(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS * 2)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(logCmd.exec(any())).thenAnswer(inv -> {
            com.github.dockerjava.api.async.ResultCallback.Adapter<Frame> cb = inv.getArgument(0);
            cb.onNext(new Frame(StreamType.STDOUT, huge));
            cb.onComplete();
            return cb;
        });

        String logs = reconciler.tailLogs(ext, 2000);

        assertThat(logs.length()).isLessThan(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS + 256);
        assertThat(logs).contains("truncated");
    }

    // The workload carries the owner stamp the read paths check (managed-by + tenant id + extension id);
    // baseLabels is what the planner puts on a real container.
    private Map<String, String> containerLabels(Extension owner, String specHash) {
        Map<String, String> labels = new HashMap<>();
        labels.put(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash);
        labels.put(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY);
        if (owner != null) {
            labels.putAll(ExtensionResourceNames.baseLabels(owner));
        }
        return labels;
    }

    private void containerWithDaemonError(Extension owner, String specHash, String daemonError) {
        InspectContainerResponse resp = mock(InspectContainerResponse.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getLabels()).thenReturn(containerLabels(owner, specHash));
        when(resp.getConfig()).thenReturn(config);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(state.getRunning()).thenReturn(false);
        // "created" reaches the fall-through: not exited, not dead, not restarting, not OOMKilled
        when(state.getStatus()).thenReturn("created");
        when(state.getError()).thenReturn(daemonError);
        when(resp.getState()).thenReturn(state);
        when(resp.getRestartCount()).thenReturn(0);
        when(inspectContainerCmd.exec()).thenReturn(resp);
    }

    @Test
    void stoppedExtensionDescribesDesiredZero() {
        Extension ext = extension(ExtensionState.STOPPED);
        containerPresent(ext, specHash(ext), false, null, "created", null);

        DeploymentHealth health = reconciler.describe(ext);

        assertThat(health.desiredReplicas()).isZero();
    }

    @Test
    void latestTagTriggersPull() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        ext.getContainerSpec().setTag("latest");
        containerAbsent();

        reconciler.apply(ext);

        verify(client).pullImageCmd("registry.example.com/acme/billing-ext");
        verify(pullCmd).withTag("latest");
    }

    // Image presence is a property of the shared Docker host, not of the requesting tenant: reusing a
    // locally cached image would let a tenant run an image:tag another tenant pulled with credentials
    // it does not hold. The pull is where those credentials are checked, so it must not be skipped.
    @Test
    void pinnedTagStillPullsEvenWhenImageIsCachedLocally() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();

        reconciler.apply(ext);

        verify(client).pullImageCmd("registry.example.com/acme/billing-ext");
        verify(pullCmd).withTag("1.4.0");
    }

    @Test
    void alwaysPullDisabledReusesCachedImageForPinnedTag() {
        DefaultExtensionDockerReconciler offline =
                new DefaultExtensionDockerReconciler(client, NETWORK, planner, 10, 1000L, 1000L, false);
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();

        offline.apply(ext);

        verify(client, never()).pullImageCmd(anyString());
    }

    @Test
    void applyRefusesToTouchUnmanagedContainer() {
        // A container with the target name but WITHOUT the managed-by label (e.g. the operator router):
        // apply must refuse rather than start/stop/recreate it.
        Extension ext = extension(ExtensionState.ACTIVE);
        InspectContainerResponse resp = mock(InspectContainerResponse.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getLabels()).thenReturn(Map.of("app", "tb-ext-router"));
        when(resp.getConfig()).thenReturn(config);
        when(inspectContainerCmd.exec()).thenReturn(resp);

        assertThatThrownBy(() -> reconciler.apply(ext)).isInstanceOf(IllegalStateException.class);
        verify(client, never()).removeContainerCmd(anyString());
        verify(client, never()).createContainerCmd(anyString());
        verify(client, never()).stopContainerCmd(anyString());
    }

    // The guard was only wired into apply(), so a tenant whose slug collided with an operator-owned
    // container could still stop, restart or force-remove it through the lifecycle actions. Every path
    // that resolves a container by its derived name must refuse a foreign one.
    private void unmanagedContainerExists() {
        InspectContainerResponse resp = mock(InspectContainerResponse.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getLabels()).thenReturn(Map.of("app", "tb-ext-router"));
        when(resp.getConfig()).thenReturn(config);
        when(inspectContainerCmd.exec()).thenReturn(resp);
    }

    @Test
    void scaleRefusesToTouchUnmanagedContainer() {
        unmanagedContainerExists();

        assertThatThrownBy(() -> reconciler.scaleTo(extension(ExtensionState.STOPPED), 0))
                .isInstanceOf(IllegalStateException.class);
        verify(client, never()).stopContainerCmd(anyString());
        verify(client, never()).startContainerCmd(anyString());
    }

    @Test
    void restartRefusesToTouchUnmanagedContainer() {
        unmanagedContainerExists();

        assertThatThrownBy(() -> reconciler.restart(extension(ExtensionState.ACTIVE)))
                .isInstanceOf(IllegalStateException.class);
        verify(client, never()).restartContainerCmd(anyString());
    }

    // Delete skips instead of throwing: teardown must still clear what we do own, and the orphan sweep
    // would otherwise retry against the same collision forever. Removal here is forced, so acting on a
    // foreign container would destroy operator infrastructure outright.
    @Test
    void deleteSkipsUnmanagedContainerWithoutRemovingIt() {
        unmanagedContainerExists();

        reconciler.deleteBySlug(SLUG);

        verify(client, never()).removeContainerCmd(anyString());
    }

    @Test
    void deleteRemovesOwnManagedContainer() {
        containerPresent("some-hash", true, "healthy", "running", null);

        reconciler.deleteBySlug(SLUG);

        verify(client).removeContainerCmd(NAME);
    }

    // A slug is released the moment its row is deleted, while the container teardown runs asynchronously —
    // and once a new row owns the slug the orphan sweep leaves the old container alone. Matching on the
    // name alone therefore hands the new extension the PREVIOUS tenant's container.
    private Extension previousOwnerOfTheSameSlug() {
        Extension other = extension(ExtensionState.ACTIVE);
        other.setSlug(SLUG); // same slug, different tenant and extension id
        return other;
    }

    @Test
    void describeIgnoresAContainerThatBelongsToAPreviousOwnerOfTheSlug() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerPresent(previousOwnerOfTheSameSlug(), specHash(ext), true, "healthy", "running", null);

        assertThat(reconciler.describe(ext).exists()).isFalse();
    }

    @Test
    void tailLogsRefusesAContainerThatBelongsToAPreviousOwnerOfTheSlug() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerPresent(previousOwnerOfTheSameSlug(), specHash(ext), true, "healthy", "running", null);

        assertThat(reconciler.tailLogs(ext, 200)).contains("No container found");
        verify(client, never()).logContainerCmd(anyString());
    }

    // The name is all that identifies a container, so an unmanaged one of the operator's answers to it too.
    @Test
    void tailLogsRefusesAnUnmanagedContainer() {
        unmanagedContainerExists();

        assertThat(reconciler.tailLogs(extension(ExtensionState.ACTIVE), 200)).contains("No container found");
        verify(client, never()).logContainerCmd(anyString());
    }

    // The spec-hash covers the container spec only, so the previous owner's container can carry the very
    // hash this extension wants. Adopting it would route one tenant's traffic into another tenant's
    // running container; the recreate also restamps the owner labels the read paths check.
    @Test
    void applyRecreatesAContainerOfAPreviousOwnerEvenAtTheSameHash() {
        Extension ext = extension(ExtensionState.ACTIVE);
        containerPresent(previousOwnerOfTheSameSlug(), specHash(ext), true, "healthy", "running", null);

        reconciler.apply(ext);

        verify(client).removeContainerCmd(NAME);
        verify(client).createContainerCmd("registry.example.com/acme/billing-ext:1.4.0");
    }

    // docker-java pre-fills every pull/create command with DockerClientConfig.effectiveAuthConfig(), i.e.
    // the credentials of the TB process's own Docker config (~/.docker/config.json, DOCKER_* env). Left in
    // place, a tenant that merely names a private image the host is logged in to gets it pulled with the
    // operator's credentials. Clearing the command's auth is what drops the X-Registry-Auth header.
    @Test
    void pullOfAPublicImageSendsNoAmbientCredentials() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        containerAbsent();

        reconciler.apply(ext);

        verify(pullCmd).withAuthConfig(null);
        verify(createContainerCmd).withAuthConfig(null);
    }

    @Test
    void pullOfAPrivateImageUsesTheExtensionsOwnCredentials() {
        Extension ext = extension(ExtensionState.PROVISIONING);
        RegistryAuth auth = new RegistryAuth();
        auth.setRegistry("registry.example.com");
        auth.setUsername("robot$acme");
        auth.setPassword("s3cret");
        ext.getContainerSpec().setRegistryAuth(auth);
        containerAbsent();

        reconciler.apply(ext);

        ArgumentCaptor<AuthConfig> captor = ArgumentCaptor.forClass(AuthConfig.class);
        verify(pullCmd).withAuthConfig(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getUsername()).isEqualTo("robot$acme");
        assertThat(captor.getValue().getRegistryAddress()).isEqualTo("registry.example.com");
    }

    @Test
    void retryClassification() {
        assertThat(DefaultExtensionDockerReconciler.isRetryableDocker(new InternalServerErrorException("boom"))).isTrue();
        assertThat(DefaultExtensionDockerReconciler.isRetryableDocker(new RuntimeException(new IOException("conn")))).isTrue();
        assertThat(DefaultExtensionDockerReconciler.isRetryableDocker(new NotFoundException("missing"))).isFalse();
        assertThat(DefaultExtensionDockerReconciler.isRetryableDocker(new NotModifiedException("already"))).isFalse();
        assertThat(DefaultExtensionDockerReconciler.isRetryableDocker(new IllegalStateException("x"))).isFalse();
    }

}
