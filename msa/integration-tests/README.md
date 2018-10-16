
## Integration tests execution
To run the integration tests with using Docker, the local Docker images of Thingsboard's microservices should be built. <br />
- Build the local Docker images in the directory with the Thingsboard's main [pom.xml](./../../pom.xml):
        
        mvn clean install -Ddockerfile.skip=false
- Verify that the new local images were built: 

        docker image ls
As result, in REPOSITORY column, next images should be present:
        
        local-maven-build/tb-node
        local-maven-build/tb-web-ui
        local-maven-build/tb-web-ui 

- Run the integration tests in the [msa/integration-tests](../integration-tests) directory:

        mvn clean install -Dintegrationtests.skip=false