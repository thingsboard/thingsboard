var mqtt = require('mqtt');
var client  = mqtt.connect('mqtt://127.0.0.1',{
    username: "UTJIeCLimeVYAZzAPWBz"
});

client.on('connect', function () {
    console.log('connected');
    client.subscribe('v1/devices/me/rpc/request/+')
});

client.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    var requestId = topic.slice('v1/devices/me/rpc/request/'.length);
	var responseTest = {"method": "Test"}
    //client acts as an echo service
    client.publish('v1/devices/me/rpc/response/' + requestId, JSON.stringify(responseTest));
});
