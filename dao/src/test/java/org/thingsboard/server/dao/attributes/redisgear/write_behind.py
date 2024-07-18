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

# cat > /usr/local/redisgear/scripts/write_behind.py
# ^D
# redis-cli RG.PYEXECUTE "$(cat /usr/local/redisgear/scripts/write_behind.py)"

import os
import psycopg2
from psycopg2 import pool
import logging

logging.basicConfig(level=logging.INFO)

# PostgreSQL connection pool
pg_pool = None

def initialize_pool():
    global pg_pool
    if pg_pool is None:
        pg_pool = psycopg2.pool.SimpleConnectionPool(1, 20,
                                                     dbname=os.getenv("PG_DBNAME", "postgres"),
                                                     user=os.getenv("PG_USER", "postgres"),
                                                     password=os.getenv("PG_PASSWORD", "postgres"),
                                                     host=os.getenv("PG_HOST", "localhost")
                                                     )
def parse_key(key):
    # "attributes{c870c1f0-13a1-11ef-8f1e-7992221af0b1}3_69"

    # Define the constant prefix and its length
    # prefix = "attributes"
    prefix_length = 10 # len(prefix)

    # Extract the entity_id (UUID with fixed length of 36 characters)
    entity_start = prefix_length + 1  # Start after "attributes{"
    entity_end = entity_start + 36    # UUID length is fixed at 36 characters
    entity_id = key[entity_start:entity_end]

    # Extract the scope_id (single digit) and keyId
    scope_id_start = entity_end + 1   # Start after the closing '}'
    scope_id_end = scope_id_start + 1 # Single digit
    scope_id = key[scope_id_start:scope_id_end]

    key_id = key[scope_id_end + 1:]   # Remaining part after the underscore '_'

    return entity_id, scope_id, key_id

def parse_value(value):
    TS_LENGTH = 13

    # Extract fixed parts
    timestamp = value[:TS_LENGTH].strip()
    data_type = value[TS_LENGTH + 1]
    has_value = value[TS_LENGTH + 3]

    # Extract the variable part if value is present
    if has_value == '1':
        variable = value[TS_LENGTH + 5:]
    else:
        variable = None

    # logging.info(f"Parsed value: timestamp={timestamp}, data_type={data_type}, has_value={has_value}, value={variable}")

    return timestamp, data_type, has_value, variable

def update_pg(x):
    initialize_pool()
    conn = pg_pool.getconn()
    cursor = conn.cursor()
    try:
        # logging.info(f"Record: {x}")
        key = x['value']['key']
        entity_id, scope_id, key_id = parse_key(key)

        event = x['value']['event']
        if event == 'set':
            value = x['value']['value']
            timestamp, data_type, has_value, variable = parse_value(value)
            last_update_ts = timestamp
            str_v, long_v, dbl_v, bool_v, json_v = None, None, None, None, None

            # Set appropriate value based on data type
            if data_type == '0':
                bool_v = variable
            elif data_type == '1':
                long_v = variable
            elif data_type == '2':
                dbl_v = variable
            elif data_type == '3':
                str_v = variable
            elif data_type == '4':
                json_v = variable
            else:
                logging.error(f"data_type unknown: data_type={data_type}")

            try:

                query = """
                INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, cast(%s AS json), %s) 
                ON CONFLICT (entity_id, attribute_type, attribute_key) 
                DO UPDATE SET str_v = %s, long_v = %s, dbl_v = %s, bool_v = %s, json_v = cast(%s AS json), last_update_ts = %s;
                """

                cursor.execute(query, (entity_id, scope_id, key_id, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts,
                                       str_v, long_v, dbl_v, bool_v, json_v, last_update_ts))

                # cursor.execute(
                #     "INSERT INTO your_table (key, value) VALUES (%s, %s) ON CONFLICT (key) DO UPDATE SET value = %s",
                #     (attribute_name, variable, variable)
                # )
                conn.commit()
                # logging.info(f"Successfully updated PostgreSQL key={key}, value={value}")
            except Exception as e:
                logging.error(f"Error updating PostgreSQL: {e}")
                conn.rollback()
        # if event == 'del':
            # logging.info(f"Ignoring del event")
    except Exception as e:
        logging.error(f"Error processing record {x}: {e}")
    finally:
        cursor.close()
        pg_pool.putconn(conn)

logging.info(f"Loading write behind...")

# Register the Gears function to process the stream
gb = GearsBuilder('StreamReader')
gb.foreach(update_pg)
gb.register('attr_kv_stream', mode='async')

logging.info(f"Loaded write behind!")
