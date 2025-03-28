package org.thingsboard.server.common.data;

import lombok.Data;

@Data  // Lombok annotation để tự động generate getter/setter
public class DeviceSettingRequest {
    private String deviceName;
    private String deviceDescription;
    private String deviceType;
    private Long minCircleNotification;
    private String wifiSsid;
    private String wifiPass;
    private Float minTemperature;
    private Float minHumidity;
    private Float maxTemperature;
    private Float maxHumidity;
    private Float minQuality;
    private Float maxQuality;
    private Float minRain;
    private Float maxRain;
    private Float minLight;
    private Float maxLight;
}