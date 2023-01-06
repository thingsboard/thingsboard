
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

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with Redis standalone:

        mvn clean install -DblackBoxTests.skip=false

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory with Redis cluster:

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.redisCluster=true

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory in Hybrid mode (postgres + cassandra):

        mvn clean install -DblackBoxTests.skip=false -DblackBoxTests.hybridMode=true

- To run the black box tests with using local env run tests in the [msa/black-box-tests](../black-box-tests) directory with runLocal property:

        mvn clean install -DblackBoxTests.skip=false -DrunLocal=true

- To run ui smoke tests in the [msa/black-box-tests](../black-box-tests) directory specifying suite name: 

        mvn clean install -DblackBoxTests.skip=false -Dsuite=uiTests

- To run all tests in the [msa/black-box-tests](../black-box-tests) directory specifying suite name:

        mvn clean install -DblackBoxTests.skip=false -Dsuite=all 

### To run a separate test manually on a built UI:
1. Add the black-box-tests module in the [pom.xml](../pom.xml)
2. Add Vm Option "-DrunLocal=true -Dtb.baseUiUrl=http://localhost:4200/" in Run -> Edit Configuration -> Edit Configuration Templates -> TestNG
3. Go to the test class you need in the [UI tests directory](../black-box-tests/src/test/java/org/thingsboard/server/msa/ui/tests) and run the test in it 
or go to the [resources](../black-box-tests/src/test/resources) and run test suite you need