package org.thingsboard.server.dao.edq;

import lombok.Getter;
import org.thingsboard.server.common.data.Device;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceRepoData {
    @Getter
    private final Device device;
    @Getter
    private final Map<Integer, String> attrs = new ConcurrentHashMap<>();

    public DeviceRepoData(Device device) {
        this.device = device;
    }

    public void putAttr(Integer keyId, String value){
        attrs.put(keyId, value);
    }
}
