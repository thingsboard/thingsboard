package org.thingsboard.server.dao.util;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;

public class DeviceConnectivityUtil {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String MQTT = "mqtt";
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    public static final String COAPS = "coaps";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";

    public static String getCurlCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    public static String getMosquittoPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials, String payload) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile tb-server-chain.pem");
        }
        command.append(" -h ").append(host).append(port == null ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            case X509_CERTIFICATE:
                if (MQTTS.equals(protocol)) {
                    return CHECK_DOCUMENTATION;
                }
            default:
                return null;
        }
        command.append(payload);
        return command.toString();
    }

    public static String getCoapClientCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String client = COAPS.equals(protocol) ? "coap-client-openssl -v 9" : "coap-client";
                return String.format("%s -m POST %s://%s%s/api/v1/%s/telemetry -t json -e %s",
                        client, protocol, host, port, deviceCredentials.getCredentialsId(), JSON_EXAMPLE_PAYLOAD);
            case X509_CERTIFICATE:
                if (COAPS.equals(protocol)) {
                    return CHECK_DOCUMENTATION;
                }
            default:
                return null;
        }
    }
}
