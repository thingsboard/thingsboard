package org.thingsboard.server.transport.lwm2m;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.transport.lwm2m.client.LwM2mDeviceEmulator;

import java.io.IOException;

@RunWith(SpringRunner.class)
public class Server_test {

    LwM2mDeviceEmulator lwM2mDeviceEmulator;

    @Test
    public void Test1() throws IOException {

        lwM2mDeviceEmulator = new LwM2mDeviceEmulator();
        System.out.println(lwM2mDeviceEmulator );
//        Process p = Runtime.getRuntime().exec("javac /home/nick/Igor_project/thingsboard_ce/thingsboard_ce/common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/client/LwM2mDeviceEmulator.java");
//

    }
}
