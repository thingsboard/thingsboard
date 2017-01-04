/**
 * Copyright © 2016 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
  * Copyright © 2016 The Thingsboard Authors
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.thingsboard.client.tools

import io.gatling.core.Predef._
import org.fusesource.mqtt.client.QoS
import scala.concurrent.duration._

import com.github.mnogu.gatling.mqtt.Predef._

class MqttSimulation extends Simulation {

  val mqttConf = mqtt
    .host("tcp://localhost:1883")
    .userName("${deviceCredentialsId}")

  val connect = exec(mqtt("connect")
      .connect())

  val publish = repeat(400) {
    exec(mqtt("publish")
      .publish("v1/devices/me/telemetry", "{\"key1\":\"value1\", \"key2\":\"value2\"}", QoS.AT_LEAST_ONCE, retain = false))
  }

  val scn = scenario("Scenario Name")
    .feed(csv("/tmp/mqtt.csv").circular)
    .exec(connect, publish)

  setUp(
      scn
        .inject(constantUsersPerSec(25) during (1 seconds))
  ).protocols(mqttConf)

}