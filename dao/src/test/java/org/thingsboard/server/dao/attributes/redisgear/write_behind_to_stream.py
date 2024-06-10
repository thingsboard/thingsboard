#
# Copyright © 2016-2024 The Thingsboard Authors
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

# cat > /usr/local/redisgear/scripts/write_behind_stream.py
# ^D
# redis-cli RG.PYEXECUTE "$(cat /usr/local/redisgear/scripts/write_behind_stream.py)"
# select * from attribute_kv where entity_id = 'c870c1f0-13a1-11ef-8f1e-7992221af0b1' order by attribute_type, attribute_key;

import os
import time
import logging

logging.basicConfig(level=logging.INFO)

def write_to_stream(record):
    # logging.info(f"write_to_stream : {record}")
    key = record['key']
    event = record['event']
    if event in ('set', 'del'):
        stream_entry = {
            'key': key,
            'event': event
            # ,
            # 'timestamp': int(time.time() * 1000)  # Current time in milliseconds
        }
        if event == 'set':
            value = record['value']
            stream_entry['value'] = value
        execute('XADD', 'attr_kv_stream', '*', *sum(stream_entry.items(), ()))
        # logging.info(f"Stream entry added: {stream_entry}")

# Register the Gears function to listen for `SET` and `DEL` events on keys with prefix `attr_kv`
gb = GearsBuilder('KeysReader')
gb.foreach(write_to_stream)
gb.register(prefix='attributes*', eventTypes=['set', 'del'], mode='sync')

logging.info(f"Stream producer registered: {gb}")
