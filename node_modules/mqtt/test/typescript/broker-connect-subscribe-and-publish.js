"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// relative path uses package.json {"types":"types/index.d.ts", ...}
var __1 = require("../..");
var BROKER = 'test.mosquitto.org';
var PAYLOAD = 'hello from TS';
var TOPIC = 'typescript-test-' + Math.random().toString(16).substr(2);
var opts = {};
console.log("connect(" + JSON.stringify(BROKER) + ")");
var client = __1.connect("mqtt://" + BROKER, opts);
client.subscribe((_a = {}, _a[TOPIC] = 2, _a), function (err, granted) {
    granted.forEach(function (_a) {
        var topic = _a.topic, qos = _a.qos;
        console.log("subscribed to " + topic + " with qos=" + qos);
    });
    client.publish(TOPIC, PAYLOAD, { qos: 2 });
}).on('message', function (topic, payload) {
    console.log("message from " + topic + ": " + payload);
    client.end();
}).on('connect', function (packet) {
    console.log('connected!', JSON.stringify(packet));
});
var _a;
//# sourceMappingURL=broker-connect-subscribe-and-publish.js.map