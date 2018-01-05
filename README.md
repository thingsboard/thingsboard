# ThingsBoard 
[![Join the chat at https://gitter.im/thingsboard/chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/thingsboard/chat?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/thingsboard/thingsboard.svg?branch=master)](https://travis-ci.org/thingsboard/thingsboard)

ThingsBoard is an open-source IoT platform for data collection, processing, visualization, and device management.

<img src="./img/logo.png?raw=true" width="100" height="100">

## Documentation

ThingsBoard documentation is hosted on [thingsboard.io](https://thingsboard.io/docs).

## IoT use cases

[**Smart metering**](https://thingsboard.io/smart-metering/)
[![Smart metering](https://user-images.githubusercontent.com/8308069/29627611-4125eebc-883b-11e7-8862-f29419902079.gif "Smart metering")](https://thingsboard.io/smart-metering/)

[**Smart energy**](https://thingsboard.io/smart-energy/)
[![Smart energy](https://cloud.githubusercontent.com/assets/8308069/24495682/aebd45d0-153e-11e7-8de4-7360ed5b41ae.gif "Smart energy")](https://thingsboard.io/smart-energy/)

[**Smart farming**](https://thingsboard.io/smart-farming/)
[![Smart farming](https://cloud.githubusercontent.com/assets/8308069/24496824/10dc1144-1542-11e7-8aa1-5d3a281d5a1a.gif "Smart farming")](https://thingsboard.io/smart-farming/)

[**Fleet tracking**](https://thingsboard.io/fleet-tracking/)
[![Fleet tracking](https://cloud.githubusercontent.com/assets/8308069/24497169/3a1a61e0-1543-11e7-8d55-3c8a13f35634.gif "Fleet tracking")](https://thingsboard.io/fleet-tracking/)

## Getting Started

Collect and Visualize your IoT data in minutes by following this [guide](https://thingsboard.io/docs/getting-started-guides/helloworld/).

# Depth Data Support 

Thingsboard can now support depthseries data also. Earlier thingsboard supported timeseries data i.e timestamp based telemetry data, but there is a need for the support of depthseries data also i.e depth based telemetry data.

## Configurations

1. There is only one configuration to be made, it is in thingsboard.yml. Under the heading UI Related configuration set depthSeries to true like:
```
#UI Related Configuration
  configurations:
    ui:
      depthSeries: "true"
```

## Build

1. Checkout branch "save_depthdata_to_thingsboardDb".
2. Do : mvn clean install -DskipTests

## Add a rule for depthseries data

1. Create a new rule similar to system telemetry rule with message type filter, just set the message types as "POST_TELEMETRY_DEPTH" for filter. Save and start the rule.

## Publish Depthseries Data to thingsboard

1. Presently depthseries data can be published to thingsboard via MQTT only. The MQTT topic for publishing depthseries data is:
   **v1/devices/me/depth/telemetry**

2. The JSON format to publish depthseries data to above topic

* Depth data as JSON array.
```json
   [{"ds":3000.1,"values":{"viscosity":0.5, "humidity":72.0}}, {"ds":3000.2,"values":{"viscosity":0.7, "humidity":69.0}}]
```
* Depth data as JSON object.
```json
   {"ds":5844.23,"values":{"viscosity":0.1, "humidity":22.0}}
```
   **Note:** the json has ds for depth instead of ts which is for timestamp.

3. Now depthseries data can be published to thingsboard similar to timeseries telemetry data using mqtt publish.

## Visualize depthseries data

### LATEST depthseries data

1. Latest values can be seen on the respective devices on which the depthseries data has been published.
2. It can also be visualized on widgets: analog and digital gauges.
* For that create/use a new/existing dashboard. Use the following link for that
  -[Getting Started](https://thingsboard.io/docs/getting-started-guides/helloworld/)
  Go to the heading "Create new dashboard to visualize the data".

### REALTIME OR HISTORICAL depthseries data

1. Go to the the dashboard created/used in the previous step.
2. Create a new widget of chart type as "new depthseries flot".
3. Add datasources with Entity alias as device name and then select from depthseries keys.
4. Click add. A "new depthseries flot" would get displayed on dashboard for realtime depthseries data for the selected key(s)(It needs realtime depthseries data being published to thingsboard).
5. For historical data go to history tab of the depthwindow panel present at top right of the dashboard(A clock icon).
6. Set the required range of depth and click update.
7. Now the depthseries data for the specified range and the selected key(s) would get plotted on the "new depthseries flot".
**Note:** Keep data aggregation function in depthwindow panel as "None".

## Support

 - [Community chat](https://gitter.im/thingsboard/chat)
 - [Q&A forum](https://groups.google.com/forum/#!forum/thingsboard)
 - [Stackoverflow](http://stackoverflow.com/questions/tagged/thingsboard)

## Licenses

This project is released under [Apache 2.0 License](./LICENSE).
