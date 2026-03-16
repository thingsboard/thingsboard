
## Running tests in parallel with a reasonable memory usage

```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"

mvn clean install -T6 -DskipTests
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
mvn test -pl dao -Dparallel=packages -DforkCount=4

mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.controller.**'      -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.edge.**'            -DforkCount=4 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.service.**'         -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.mqtt.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.coap.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.lwm2m.**' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='**/*TestSuite.java'                                     -DforkCount=4 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

#the rest of application tests
mvn test -pl application -Dtest='
!**/nosql/**,
!org.thingsboard.server.controller.**,
!org.thingsboard.server.edge.**,
!org.thingsboard.server.service.**,
!org.thingsboard.server.transport.mqtt.**,
!org.thingsboard.server.transport.coap.**,
!org.thingsboard.server.transport.lwm2m.**,
!**/*TestSuite.java
' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
```

## Testcontainers compatibility with the Docker API workaround

In case your tests failed to run testcontainers due to unsupported Docker API version

:coffee: testcontainers (Docker API 1.32) + :whale: docker 29 (min API 1.44) workaround

Add to /etc/docker/daemon.json and restart docker
```json
{
  "min-api-version": "1.32"
}
```

Same works on Mac, except `daemon.json` are located in another folder and required to be edited from Docker Desktop UI.

Tip: If your testcontainer are struggling to find any Docker. You can try to remove the testcontainers property file. It will be recreated on the next testcontainers run.
```bash
rm ~/.testcontainers.properties
```
