#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

import paho.mqtt.client as mqtt
from time import sleep
import random

broker="test.mosquitto.org"
topic_pub='v1/devices/me/telemetry'


client = mqtt.Client()

client.username_pw_set("TEST_TOKEN")
client.connect('127.0.0.1', 1883, 1)

for i in range(5):
    x = random.randrange(20, 100)
    print x
    msg = '{"windSpeed":"'+ str(x) + '"}'
    client.publish(topic_pub, msg)
    sleep(0.1)
