
## Black box tests execution
The tests run against the compose stack of the [thingsboard-pe-docker-compose](https://github.com/thingsboard/thingsboard-pe-docker-compose)
repository. It is cloned automatically into `~/.cache/thingsboard/compose-<branch>-<repository hash>` (override with
`-Dtb.compose.cacheDir`) at the branch pinned by the `tb.compose.ref` Maven property in [pom.xml](./pom.xml), which Maven
hands to the test JVM. When starting the containers outside Maven, pass `-Dtb.compose.ref=<branch>` explicitly. To run
against a different compose repository - a fork, or a local mirror - pass `-Dtb.compose.repository=<url>`.

Every cache records the repository it was cloned from. A cache belonging to a different repository - including one under
a `-Dtb.compose.cacheDir` path, whose name says nothing about the repository - is discarded and cloned again rather than
reused, so a run against a fork never silently gets the default repository's compose stack. The cache is discarded
before the clone, so a repository that cannot be reached at that point fails the run instead of falling back.

The repository hash is new: caches from earlier runs sit at `~/.cache/thingsboard/compose-<branch>`, are never looked
at again and are never cleaned up. Delete those un-suffixed directories once.

To run the black box tests with using Docker, the local Docker images of Thingsboard's microservices should be built. <br />
- Build the local Docker images in the directory with the Thingsboard's main [pom.xml](./../../pom.xml):
        
        mvn clean install -Ddockerfile.skip=false
- Verify that the new local images were built: 

        docker image ls

Also, for start test for MQTT integration, you need to have an **_eclipse-mosquitto_** image. For get this image, you need use:
        
        docker pull eclipse-mosquitto

Also, for start test for OPC UA integration, you need to have an **_opc-plc_** image. For get this image, you need use:

        docker pull mcr.microsoft.com/iotedge/opc-plc:2.8.5

As result, in REPOSITORY column, next images should be present:

        thingsboard/tb-coap-transport
        thingsboard/tb-lwm2m-transport
        thingsboard/tb-http-transport
        thingsboard/tb-mqtt-transport
        thingsboard/tb-snmp-transport
        thingsboard/tb-node
        thingsboard/tb-web-ui
        thingsboard/tb-js-executor
        thingsboard/tb-web-report
        thingsboard/tb-http-integration
        thingsboard/tb-mqtt-integration

The suite brings its containers up keyless: `ContainerTestSuite` empties `TB_LICENSE_SECRET` and sets
`NON_PRODUCTION_USE=true` in `tb-node.env`, so no licence key, cluster id or licence server is needed to run
it. Keyless mode grants the full feature set bar white-labeling and Trendz, and marks the deployment as
development - which is why every generated report carries the non-production notice as its first row.

- Run the black box tests (without ui tests) in the [msa/black-box-tests](../black-box-tests) directory with Valkey standalone:

        mvn clean install -DblackBoxTests.skip=false

- Run the black box tests (without ui tests) in the [msa/black-box-tests](../black-box-tests) directory with Valkey standalone with TLS:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisSsl=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with the Valkey cluster:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisCluster=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with Valkey sentinel:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisSentinel=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory in Hybrid mode (postgres +
  cassandra):

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.hybridMode=true

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:
- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with integrations:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.integrations.skip=false

  Note: for AWS IOT integration add next VM options:

  -DblackBoxTests.aws.endpoint=YOUR_AWAZON_CLIENT_ENDPOINT
  -DblackBoxTests.aws.rootCA=PATH_TO_ROOT_CA_PEM
  -DblackBoxTests.aws.cert=PATH_TO_CERT
  -DblackBoxTests.aws.privateKey=PATH_TO_PRIVATE_KEY
  
  Note: for Azure IoT Hub integration add next VM options:
        
  -DblackBoxTests.azureIotHubHostName=YOUR_HOST_NAME
  -DblackBoxTests.azureIotHubDeviceId=YOUR_DEVICE_ID
  -DblackBoxTests.azureIotHubSasKey=YOUR_SAS_KEY
  -DblackBoxTests.azureIotHubConnectionString=YOUR_CONNECTION_STRING
  [connection string](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-java-java-c2d#get-the-iot-hub-connection-string)
 
 Note: for Azure Event Hub integration add next VM options:

  -DblackBoxTests.azureEventHubConnectionString=YOUR_CONNECTION_STRING

  Note: for Azure Service Bus integration add next VM options:

  -DblackBoxTests.azureServiceBusConnectionString=YOUR_CONNECTION_STRING
  -DblackBoxTests.azureServiceBusTopicName=YOUR_TOPIC_NAME
  -DblackBoxTests.azureServiceBusSubName=YOUR_SUB_NAME
  -DblackBoxTests.azureServiceBusDownlinkConnectionString=YOUR_CONNECTION_STRING
  -DblackBoxTests.azureServiceBusDownlinkTopicName=YOUR_TOPIC_NAME 
  -DblackBoxTests.azureServiceBusDownlinkSubName=YOUR_SUB_NAME

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:

        mvn clean install -DblackBoxTests.skip=false -DrunLocal=true

- To run only integrations tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=integrations

- To run only connectivity tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=connectivity

- To run only ui tests in the [msa/black-box-tests](../black-box-tests) directory: 

        mvn clean install -DblackBoxTests.skip=false -Dsuite=uiTests

- To run only ui smoke rule chains tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=smokesRuleChain

- To run only ui smoke customers tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=smokesCustomer

- To run only ui smoke profiles tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=smokesPrifiles

- To run all tests (black-box and ui) in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=all 

### To run a separate test manually on a built UI:
1. Add the black-box-tests module in the [pom.xml](../pom.xml) or add as a Maven project
2. Add Vm Option "*-DrunLocal=true -Dtb.baseUiUrl=http://localhost:4200/*" in "Run" -> "Edit Configuration" -> "Edit Configuration Templates" -> "TestNG"
3. To run a specific test, go to the test class in the [UI tests package](../black-box-tests/src/test/java/org/thingsboard/server/msa/ui/tests) and run the test. Alternatively, go to the [resources](../black-box-tests/src/test/resources) in the black-box-tests module and run the test suite that you need.