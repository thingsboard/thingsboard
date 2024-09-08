package org.thingsboard.server.msa.connectivity.lwm2m;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.msa.connectivity.lwm2m.client.LwM2MTestClient;

@Slf4j
@Data
public class Lwm2mDevicesForTest {

    Device lwM2MDeviceTest;
    LwM2MTestClient lwM2MTestClient;
    DeviceProfile lwm2mDeviceProfile;
    public Lwm2mDevicesForTest(DeviceProfile deviceProfile) {
        this.lwm2mDeviceProfile = deviceProfile;
    }
}
