var mqtt = require('mqtt');

// Don't forget to update accessToken constant with your device access token
const thingsboardHost = "127.0.0.1";
const ACCESS_TOKEN = "RmgQMjl0I1oCMIRLbdEy";
const minDirection = 0, maxDirection = 359;


// Initialization of mqtt client using Thingsboard host and device access token
console.log('Connecting to: %s using access token: %s', thingsboardHost, ACCESS_TOKEN);
var client  = mqtt.connect('mqtt://'+ thingsboardHost, { username: ACCESS_TOKEN });

// Initialization of wind direction data with random values
 var  windDirection = minDirection + (maxDirection - minDirection) * Math.random();

// Triggers when client is successfully connected to the Thingsboard server
client.on('connect', function () {
    console.log('Client connected!');
    // Schedules telemetry data upload once per five second
    console.log('Uploading  wind direction data once per second...');
    setInterval(publishTelemetry, 5000);
});

// Uploads telemetry data using 'v1/devices/me/telemetry' MQTT topic
function publishTelemetry() {
    windDirection = genNextValue(windDirection, minDirection, maxDirection);
    console.log('Sending: ' + JSON.stringify({windDirection: windDirection}));
    client.publish('v1/devices/me/telemetry', JSON.stringify({windDirection: windDirection}));
}

// Generates new random value that is within 3% range from previous value
function genNextValue(prevValue, min, max) {
    var value = prevValue + ((max - min) * (Math.random() - 0.5)) * 0.03;
    value = Math.max(min, Math.min(max, value));
    return Math.round(value * 10) / 10;
}

//Catches ctrl+c event
process.on('SIGINT', function () {
    console.log();
    console.log('Disconnecting...');
    client.end();
    console.log('Exited!');
    process.exit(2);
});

//Catches uncaught exceptions
process.on('uncaughtException', function(e) {
    console.log('Uncaught Exception...');
    console.log(e.stack);
    process.exit(99);
});
