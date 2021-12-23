var mqtt = require('mqtt');

// Don't forget to update accessToken constant with your device access token
const thingsboardHost = "127.0.0.1";
const ACCESS_TOKEN = "p3iyq8jqio5I7WsqDwiW";
const minDirection = 0, maxDirection = 360;


// Initialization of mqtt client using Thingsboard host and device access token
console.log('Connecting to: %s using access token: %s', thingsboardHost, ACCESS_TOKEN);
var client  = mqtt.connect('mqtt://'+ thingsboardHost, { username: ACCESS_TOKEN });

var value = 350;
var spinFlag = {method: "spinRight", params: 0};

//RPC message handling sent to the client
client.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    var tmp = JSON.parse(message.toString());
    if (tmp.method == "spinRight") {
        spinFlag = tmp;
        // Uploads telemetry data using 'v1/devices/me/telemetry' MQTT topic
        client.publish('v1/devices/me/telemetry', JSON.stringify({spinFlag: "rotating right"}));
    }
    if (tmp.method == "spinLeft") {
        spinFlag = tmp;
        // Uploads telemetry data using 'v1/devices/me/telemetry' MQTT topic
        client.publish('v1/devices/me/telemetry', JSON.stringify({spinFlag: "rotating left"}));
    }
    var requestId = topic.slice('v1/devices/me/rpc/request/'.length);
    //client acts as an echo service
    client.publish('v1/devices/me/rpc/response/' + requestId, message);

});

// Triggers when client is successfully connected to the Thingsboard server
client.on('connect', function () {
    console.log('Client connected!');
    client.subscribe('v1/devices/me/rpc/request/+');
    // Schedules telemetry data upload once per five second
    console.log('Uploading data once per second...');
    setInterval(publishTelemetry, 5000);
});

// Uploads telemetry data using 'v1/devices/me/telemetry' MQTT topic
function publishTelemetry() {
    emulationTurbineDirectionChanging();
    console.log('Sending: ' + JSON.stringify({turbineDirection: value}));
    client.publish('v1/devices/me/telemetry', JSON.stringify({turbineDirection: value}));
}

function emulationTurbineDirectionChanging() {
    if (value >= maxDirection) {
        value = value - maxDirection;
    } else if (value <= (minDirection)) {
        value = maxDirection + value;
    }
    console.log('spinFlag - : ' + spinFlag.method);
    console.log('windValue - : ' + spinFlag.params);
    if (Math.abs(spinFlag.params - value) > 5) {
        if (spinFlag.method == "spinLeft") {
            value += 5;
        } else if (spinFlag.method == "spinRight") {
            value -= 5;
        }
    }
    return value = Math.round(value * 10) / 10;
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
process.on('uncaughtException', function (e) {
    console.log('Uncaught Exception...');
    console.log(e.stack);
    process.exit(99);
});

