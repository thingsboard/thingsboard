
## Running tests in parallel with a reasonable memory usage

```bash
mvn clean install -T6 -DskipTests
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
mvn test -pl dao -Dparallel=packages -DforkCount=4

mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.controller.**'      -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.edge.**'            -DforkCount=4 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.service.**'         -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.transport.mqtt.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.transport.coap.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='org.thingsboard.server.transport.lwm2m.**' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dsurefire.excludes='**/nosql/*Test.java' -Dtest='**/*TestSuite.java'                        -DforkCount=4 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

#the rest of application tests
mvn test -pl application -Dtest='
!**/nosql/*Test.java,
!org.thingsboard.server.controller.**,
!org.thingsboard.server.edge.**,
!org.thingsboard.server.service.**,
!org.thingsboard.server.transport.mqtt.**,
!org.thingsboard.server.transport.coap.**,
!org.thingsboard.server.transport.lwm2m.**
' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
```
