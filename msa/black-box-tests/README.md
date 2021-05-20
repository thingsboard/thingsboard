
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
        thingsboard/tb-node
        thingsboard/tb-web-ui
        thingsboard/tb-js-executor

- Run the black box tests in the [msa/black-box-tests](../black-box-tests) directory:

        mvn clean install -DblackBoxTests.skip=false


