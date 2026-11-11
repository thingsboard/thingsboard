
var t = msg.temperature;
var h = msg.humidity;
var td = 243.04*(Math.log(h/100)+((17.625*t)/(243.04+t)))/(17.625-Math.log(h/100)-((17.625*t)/(243.04+t)));

var result = {
    temperature: t,
    humidity: h,
    dewPoint: Number(td.toFixed(2))
};

return {
    contentType: "JSON",
    data: JSON.stringify(result),
    metadata: {
        topic: integrationMetadata["topicPrefix"] + "/upload"
    }
};
