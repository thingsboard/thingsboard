# thingsboard

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 3.2.2](https://img.shields.io/badge/AppVersion-3.2.2-informational?style=flat-square)

Thingsboard Helm chart for Kubernetes

### Install / Upgrade
```shell
helm upgrade thingsboard \
  --install . \
  --namespace thingsboard \
  --create-namespace \
  -f values.yaml
```

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | cassandra | 7.6.3 |
| https://charts.bitnami.com/bitnami | kafka | 13.0.3 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| cassandra.cluster.clientEncryption | bool | `false` |  |
| cassandra.cluster.datacenter | string | `"datacenter1"` |  |
| cassandra.cluster.enableRPC | bool | `false` |  |
| cassandra.cluster.endpointSnitch | string | `"GossipingPropertyFileSnitch"` |  |
| cassandra.cluster.internodeEncryption | string | `"none"` |  |
| cassandra.cluster.name | string | `"Thingsboard Cluster"` |  |
| cassandra.cluster.numTokens | int | `256` |  |
| cassandra.cluster.rack | string | `"Rack-Thingsboard-Cluster"` |  |
| cassandra.cluster.seedCount | int | `1` |  |
| cassandra.commonAnnotations."helm.sh/hook-weight" | string | `"-4"` |  |
| cassandra.dbUser.password | string | `"cassandra"` |  |
| cassandra.dbUser.user | string | `"cassandra"` |  |
| cassandra.enabled | bool | `false` |  |
| cassandra.image.pullPolicy | string | `"IfNotPresent"` |  |
| cassandra.image.repository | string | `"bitnami/cassandra"` |  |
| cassandra.image.tag | string | `"3.11.10-debian-10-r125"` |  |
| cassandra.initDBConfigMap | string | `"thingsboard-cassandra-init"` |  |
| cassandra.jvm.maxHeapSize | string | `"1024M"` |  |
| cassandra.jvm.newHeapSize | string | `"256M"` |  |
| cassandra.persistence.enabled | bool | `true` |  |
| coapTransport.image.imagePullPolicy | string | `nil` |  |
| coapTransport.image.repository | string | `"thingsboard/tb-coap-transport"` |  |
| coapTransport.image.tag | string | `nil` |  |
| coapTransport.podAnnotations | object | `{}` |  |
| coapTransport.replicaCount | int | `1` |  |
| coapTransport.serverEnvConfigs | object | `{}` |  |
| coapTransport.service.port | int | `5683` |  |
| coapTransport.service.type | string | `"LoadBalancer"` |  |
| externalCassandra.enabled | bool | `false` |  |
| externalCassandra.hostName | string | `"cassandra.example.com"` |  |
| externalCassandra.password | string | `"cassandra"` |  |
| externalCassandra.port | int | `9042` |  |
| externalCassandra.username | string | `"cassandra"` |  |
| externalKafka.hostname | string | `"kafka.host.name"` |  |
| externalKafka.port | int | `9092` |  |
| externalKafka.zookeeper.hostname | string | `"zookeeper.host.name"` |  |
| externalKafka.zookeeper.port | int | `2181` |  |
| externalPostgres.existingSecretName | string | `""` |  |
| externalPostgres.password | string | `"postgres"` |  |
| externalPostgres.postgresUrl | string | `"jdbc:postgresql://localhost:5432/database_name"` |  |
| externalPostgres.username | string | `"postgres"` |  |
| global.fullnameOverride | string | `""` | fully override release name |
| global.image.imagePullPolicy | string | `"IfNotPresent"` |  |
| global.image.tag | string | `"3.2.2"` | global thingsboard container image to use |
| global.imagePullSecrets | list | `[]` |  |
| global.nameOverride | string | `""` |  |
| httpTransport.image.imagePullPolicy | string | `nil` |  |
| httpTransport.image.repository | string | `"thingsboard/tb-http-transport"` |  |
| httpTransport.image.tag | string | `nil` |  |
| httpTransport.podAnnotations | object | `{}` |  |
| httpTransport.replicaCount | int | `1` |  |
| httpTransport.serverEnvConfigs | object | `{}` |  |
| httpTransport.service.port | int | `8080` |  |
| httpTransport.service.type | string | `"ClusterIP"` |  |
| javascriptExecutor.image.imagePullPolicy | string | `nil` |  |
| javascriptExecutor.image.repository | string | `"thingsboard/tb-js-executor"` |  |
| javascriptExecutor.image.tag | string | `nil` |  |
| javascriptExecutor.podAnnotations | object | `{}` |  |
| javascriptExecutor.replicaCount | int | `1` |  |
| javascriptExecutor.serverEnvConfigs.DOCKER_MODE | string | `"true"` |  |
| javascriptExecutor.serverEnvConfigs.LOGGER_FILENAME | string | `"tb-js-executor-%DATE%.log"` |  |
| javascriptExecutor.serverEnvConfigs.LOGGER_LEVEL | string | `"info"` |  |
| javascriptExecutor.serverEnvConfigs.LOG_FOLDER | string | `"logs"` |  |
| javascriptExecutor.serverEnvConfigs.REMOTE_JS_EVAL_REQUEST_TOPIC | string | `"js_eval.requests"` |  |
| javascriptExecutor.serverEnvConfigs.SCRIPT_BODY_TRACE_FREQUENCY | string | `"1000"` |  |
| kafka.autoCreateTopicsEnable | bool | `false` |  |
| kafka.enabled | bool | `true` | if true local kafka will be deployed |
| kafka.image.pullPolicy | string | `"IfNotPresent"` |  |
| kafka.image.registry | string | `"docker.io"` |  |
| kafka.image.repository | string | `"bitnami/kafka"` |  |
| kafka.image.tag | string | `"2.8.0-debian-10-r43"` |  |
| kafka.logRetentionBytes | string | `"_1073741824"` |  |
| kafka.logRetentionCheckIntervalMs | string | `"300000"` |  |
| kafka.logSegmentBytes | string | `"_268435456"` |  |
| kafka.persistence.enabled | bool | `false` |  |
| kafka.provisioning.enabled | bool | `true` | provision kafka cluster |
| kafka.provisioning.topics[0].config."cleanup.policy" | string | `"delete"` |  |
| kafka.provisioning.topics[0].config."retention.bytes" | string | `"104857600"` |  |
| kafka.provisioning.topics[0].config."retention.ms" | string | `"60000"` |  |
| kafka.provisioning.topics[0].config."segment.bytes" | string | `"26214400"` |  |
| kafka.provisioning.topics[0].name | string | `"js_eval.requests"` |  |
| kafka.provisioning.topics[0].partitions | int | `100` |  |
| kafka.provisioning.topics[0].replicationFactor | int | `1` |  |
| kafka.provisioning.topics[1].config."cleanup.policy" | string | `"delete"` |  |
| kafka.provisioning.topics[1].config."retention.bytes" | string | `"104857600"` |  |
| kafka.provisioning.topics[1].config."retention.ms" | string | `"60000"` |  |
| kafka.provisioning.topics[1].config."segment.bytes" | string | `"26214400"` |  |
| kafka.provisioning.topics[1].name | string | `"tb_transport.api.requests"` |  |
| kafka.provisioning.topics[1].partitions | int | `30` |  |
| kafka.provisioning.topics[1].replicationFactor | int | `1` |  |
| kafka.provisioning.topics[2].config."cleanup.policy" | string | `"delete"` |  |
| kafka.provisioning.topics[2].config."retention.bytes" | string | `"104857600"` |  |
| kafka.provisioning.topics[2].config."retention.ms" | string | `"60000"` |  |
| kafka.provisioning.topics[2].config."segment.bytes" | string | `"26214400"` |  |
| kafka.provisioning.topics[2].name | string | `"tb_rule_engine"` |  |
| kafka.provisioning.topics[2].partitions | int | `30` |  |
| kafka.provisioning.topics[2].replicationFactor | int | `1` |  |
| kafka.service.port | int | `9092` |  |
| kafka.zookeeper.persistence.enabled | bool | `true` |  |
| kafka.zookeeper.persistence.size | string | `"8Gi"` |  |
| kafka.zookeeper.persistence.storageClass | string | `""` |  |
| mqttTransport.image.imagePullPolicy | string | `nil` |  |
| mqttTransport.image.repository | string | `"thingsboard/tb-mqtt-transport"` |  |
| mqttTransport.image.tag | string | `nil` |  |
| mqttTransport.podAnnotations | object | `{}` |  |
| mqttTransport.replicaCount | int | `1` |  |
| mqttTransport.serverEnvConfigs | object | `{}` |  |
| mqttTransport.service.port | int | `1883` |  |
| mqttTransport.service.type | string | `"LoadBalancer"` |  |
| postgres.enabled | bool | `true` | if postgres.enabled is true, helm will install local postgres database |
| postgres.existingSecretName | string | `""` |  |
| postgres.image.pullPolicy | string | `"IfNotPresent"` |  |
| postgres.image.repository | string | `"postgres"` |  |
| postgres.image.tag | int | `12` |  |
| postgres.password | string | `"postgres"` | default postgres password |
| postgres.service.port | int | `5432` |  |
| postgres.service.type | string | `"ClusterIP"` |  |
| postgres.username | string | `"postgres"` | default postgres username |
| redis.enabled | bool | `true` | if true redis will be deployed, currently this is mandatory |
| redis.highAvailabilityDeploy | bool | `false` |  |
| redis.image.pullPolicy | string | `"IfNotPresent"` |  |
| redis.image.repository | string | `"redis"` |  |
| redis.image.tag | string | `"6.2.4"` | redis version to deploy |
| redis.service.port | int | `6379` |  |
| redis.service.type | string | `"ClusterIP"` |  |
| thingsboard.affinity | object | `{}` |  |
| thingsboard.autoscaling.enabled | bool | `false` |  |
| thingsboard.autoscaling.maxReplicas | int | `100` |  |
| thingsboard.autoscaling.minReplicas | int | `1` |  |
| thingsboard.autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| thingsboard.extraNodeConfigmap | string | `nil` |  |
| thingsboard.image.imagePullPolicy | string | `nil` |  |
| thingsboard.image.repository | string | `"thingsboard/tb-node"` |  |
| thingsboard.image.tag | string | `nil` |  |
| thingsboard.ingress.annotations."kubernetes.io/ingress.class" | string | `"nginx"` |  |
| thingsboard.ingress.enabled | bool | `true` |  |
| thingsboard.ingress.hosts[0].eXtraPaths | list | `[]` |  |
| thingsboard.ingress.hosts[0].host | string | `"thingsboard.localhost"` |  |
| thingsboard.ingress.tls | list | `[]` |  |
| thingsboard.loadDemoData | bool | `true` |  |
| thingsboard.nodeSelector | object | `{}` |  |
| thingsboard.podAnnotations | object | `{}` |  |
| thingsboard.podSecurityContext | object | `{}` |  |
| thingsboard.previousVersionHack | string | `"3.2.1"` |  |
| thingsboard.replicaCount | int | `1` |  |
| thingsboard.resources | object | `{}` |  |
| thingsboard.securityContext | object | `{}` |  |
| thingsboard.service.port | int | `80` |  |
| thingsboard.service.type | string | `"ClusterIP"` |  |
| thingsboard.serviceAccount.annotations | object | `{}` |  |
| thingsboard.serviceAccount.create | bool | `true` |  |
| thingsboard.serviceAccount.name | string | `""` |  |
| thingsboard.tolerations | list | `[]` |  |
| webUi.image.imagePullPolicy | string | `nil` |  |
| webUi.image.repository | string | `"thingsboard/tb-web-ui"` |  |
| webUi.image.tag | string | `nil` |  |
| webUi.podAnnotations | object | `{}` |  |
| webUi.replicaCount | int | `1` |  |
| webUi.serverEnvConfigs | object | `{}` |  |
| webUi.service.port | int | `8080` |  |
| webUi.service.type | string | `"ClusterIP"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
