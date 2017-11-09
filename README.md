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

# Spark Annotations

## Introduction

This feature allows to add Plugin actions dynamically for Spark Computation Plugin, once Spark application jar annotated with [annotations](https://github.com/hashmapinc/TempusSparkAnnotations) is placed in configured directory. ComputationDiscoveryService is configured to scan these jars for create/modify/delete events and process them.

### Configurations

For polling 2 configurations need to be made.
1. SPARK_COMPUTATIONS_PATH : Directory to poll for jar files
2. DIRECTORY_POLLING_INTERVAL : Polling interval to scan jars from directory, in seconds.

### Annotations

Spark application jar should contain a class with below annotations to be added as a dynamic action for Plugin
1. **SparkAction** : Main annotation which will be used to build Action class, with below configurable settings.

```java
  @SparkAction(applicationKey = "TEST_APPLICATION", name = "Test Application", actionClass = "TestSparkAction", descriptor = "TestAppActionDescriptor.json")
```

  * *name* : Name to be displayed on UI for action.

  * *actionClass* : Absolute Class name of an action without package e.g. TestSparkComputationAction.

  * *applicationKey* : Application name string to be used as identifier to identitify which spark applications are running.

  * *descriptor* : Option field pointing to React JSON schema file for configuring Spark input parameters. (This json file if customized should be present under resources folder).
 
2. **Configurations** : This annotation will be used to generate Configuration class to hold spark application paramteres eneterd through action descriptor. Once generated class it will extend to *SparkComputationPluginActionConfiguration*

 * *className* : Absolute class name to be generated e.g. TestSparkComputationActionConfiguration.
 
 * *mappings* : Optional array of ConfigurationMappings to map field names to Java Types, see below.
 
3. **ConfigurationMappings** : Hold information of each member variable of Configuration Class.

 * *field* : Name of a member variable to add to Configuration class generated, e.g. zkUrl, kafkaBrokers etc.
 
 * *type* : java.lang.Class type field which will be used as data type for above field, e.g. String.class, Integer.class etc.
 
 ```java
 @Configurations(className = "TestAppConfiguration", mappings = {
        @ConfigurationMapping(field = "zkUrl", type = String.class),
        @ConfigurationMapping(field = "kafkaBrokers", type = String.class),
        @ConfigurationMapping(field = "window", type = Long.class)
})
 ```
 
4. **SparkRequest** : Annotation used to get information to build a request to be posted to start a Spark application from plugin. In will be used to build SparkComputationRequest.

 * *main* : Canonical name of a spark application main class which will be used to trigger Spark application.
 
 * *jar* : Name of jar(without group id) containing this main class e.g. arima-model-0.1.jar
 
 * *args* : Array of strings used by spark job. e.g. 
 ```java
 @SparkRequest(main = "com.hashmap.app.SparkApp", jar = "sample-spark-computation-1.0-SNAPSHOT",
        args = {"--window", "Long.toString(configuration.getWindow())", "--mqttbroker", "configuration.getEndpoint()",
                "--kafka", "configuration.getKafkaBrokers()", "--token", "configuration.getGatewayApiToken()"})
 ```
 
 ***These are all Type annotations, Processed by AnnotationsProcessor. Also package name for action and configuration is defaulted to package name of class having those annotations. e.g. Annotations are placed on class com.hashmap.tempus.app.TestSparkApp then classes generated will have the same package i.e. com.hashmap.tempus.app***

## Support

 - [Community chat](https://gitter.im/thingsboard/chat)
 - [Q&A forum](https://groups.google.com/forum/#!forum/thingsboard)
 - [Stackoverflow](http://stackoverflow.com/questions/tagged/thingsboard)

## Licenses

This project is released under [Apache 2.0 License](./LICENSE).
