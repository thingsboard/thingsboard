#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import paho.mqtt.client as mqtt
from time import sleep
import random

broker="test.mosquitto.org"
topic_pub='v1/devices/me/telemetry'


client = mqtt.Client()

client.username_pw_set("qyA3gP50SpGwfwyNGyi7")
client.connect('127.0.0.1', 1883, 1)

for i in range(100):
    x = random.randrange(20, 100)
    print x
    msg = '{"windSpeed":"'+ str(x) + '"}'
    client.publish(topic_pub, msg)
    sleep(0.1)
#while True:
#    val3 = random.uniform(0, 5)
#    val4 = random.uniform(-3, 3)#
#
#    msg = '{"key3": '+ str(val3) +', "key4": ' + str(val4) + '}'
#
#    print('Message: ' + msg)
#    client.publish(topic_pub, msg)
#
#    sleep(0.5)
