
## Black box tests execution
To run the black box tests with using Docker, the local Docker images of Thingsboard's microservices should be built. <br />
- Build the local Docker images in the directory with the Thingsboard's main [pom.xml](./../../pom.xml):
        
        mvn clean install -Ddockerfile.skip=false
- Verify that the new local images were built: 

        docker image ls
As result, in REPOSITORY column, next images should be present:
        
        thingsboard/tb-coap-transport
        thingsboard/tb-lwm2m-transport
        thingsboard/tb-http-transport
        thingsboard/tb-mqtt-transport
        thingsboard/tb-snmp-transport
        thingsboard/tb-node
        thingsboard/tb-web-ui
        thingsboard/tb-js-executor

- Run the black box tests (without ui tests) in the [msa/black-box-tests](../black-box-tests) directory with Valkey standalone:

        mvn clean install -DblackBoxTests.skip=false

- Run the black box tests (without ui tests) in the [msa/black-box-tests](../black-box-tests) directory with Valkey standalone with TLS:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisSsl=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with the Valkey cluster:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisCluster=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with Valkey sentinel:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisSentinel=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory in Hybrid mode (postgres + cassandra):

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.hybridMode=true

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:

        mvn clean install -DblackBoxTests.skip=false -DrunLocal=true

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