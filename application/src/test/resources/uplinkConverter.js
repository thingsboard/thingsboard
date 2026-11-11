var deviceName = String.fromCharCode.apply(String, payload);
var telemetryKeyName = metadata.telemetryKeyName;
return {
        deviceName: deviceName,
        telemetry: {
            telemetryKeyName: 42
        }
};
