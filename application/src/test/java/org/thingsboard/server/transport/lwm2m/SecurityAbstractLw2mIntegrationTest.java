package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.extension.TestWatcher;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.query.*;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MClientCredentialsConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@Slf4j
public abstract class SecurityAbstractLw2mIntegrationTest extends AbstractLwM2MIntegrationTest implements TestWatcher {

    public static final String SERVER_PUBLIC_KEY = "serverPublicKey";
    public static final String PORT = "port";

    private final NetworkConfig coapConfig = new NetworkConfig().setString("COAP_SECURE_PORT", Integer.toString(getPort()));
    private final String endpoint = "deviceAEndpoint";
    private final String serverUri = "coaps://localhost:" + getPort();
    private LwM2MTestClient client;

    public abstract int getPort();

    @NotNull
    public Device createDevice(String credentialsId, LwM2MClientCredentialsConfig credentialsConfig) throws Exception {
        Device device = new Device();
        device.setName("Device A");
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);

        deviceCredentials.setCredentialsId(credentialsId);

        LwM2MCredentials credentials = new LwM2MCredentials();

        credentials.setClient(credentialsConfig);

        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    public void testConnectAndObserveTelemetry(
            String transportConfiguration,
            String credentialsId,
            LwM2MClientCredentialsConfig lwM2MClientCredentialsConfig,
            Security security
    ) throws Exception {
        createDeviceProfile(transportConfiguration);

        Device device = createDevice(credentialsId, lwM2MClientCredentialsConfig);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();
        client = new LwM2MTestClient(executor, getEndpoint());
        client.init(security, coapConfig);
        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertEquals(42, Long.parseLong(tsValue.getValue()));
        client.destroy();
    }

    public void testConnectWithCertAndObserveTelemetry(
            String transportConfiguration,
            String credentialId,
            LwM2MClientCredentialsConfig lwM2MClientCredentialsConfig,
            Security security
    ) throws Exception {
        createDeviceProfile(transportConfiguration);
        Device device = createDevice(credentialId, lwM2MClientCredentialsConfig);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();
        LwM2MTestClient client = new LwM2MTestClient(executor, getEndpoint());

        client.init(security, coapConfig);
        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertEquals(42, Long.parseLong(tsValue.getValue()));
        client.destroy();
    }

    protected ObjectNode initTransportConfigurationObjectNode(String configPath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath)) {
            ObjectNode config = (ObjectNode) mapper.readTree(inputStream);
            ObjectNode lvm2mServerConfiguration = getLvm2mServerConfigurationFromTransportConfiguration(config);
            lvm2mServerConfiguration.put(PORT, getPort());
            return config;
        } catch (Exception e) {
            log.error("Error running LW2M RPK test", e);
            throw new RuntimeException(e);
        }
    }


    protected ObjectNode getLvm2mServerConfigurationFromTransportConfiguration(ObjectNode transportConfigurationJsonNode) {
        Assert.assertTrue(transportConfigurationJsonNode.has("bootstrap"));
        Assert.assertTrue(transportConfigurationJsonNode.get("bootstrap").has("lwm2mServer"));
        ObjectNode lwm2mServerConfiguration = (ObjectNode) transportConfigurationJsonNode.get("bootstrap").get("lwm2mServer");
        return lwm2mServerConfiguration;
    }
}
