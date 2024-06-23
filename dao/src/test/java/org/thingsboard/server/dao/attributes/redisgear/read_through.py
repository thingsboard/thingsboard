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

def fetch_data_on_miss(r):
    initialize_pool()
    conn = pg_pool.getconn()
    cursor = conn.cursor()
    try:
        key = r['key']
        logging.info(f"Cache miss for key={key}")
        # Fetch data from PostgreSQL
        cursor.execute("SELECT value FROM your_table WHERE key = %s", (key,))
        result = cursor.fetchone()
        if result:
            value = result[0]
            logging.info(f"Fetched from PostgreSQL key={key}, value={value}")
            # Update Redis cache
            execute('set', key, value)
            override_reply(value)
        else:
            execute('set', key, 'NullValue') # not found -- constant
            override_reply(None)
    finally:
        cursor.close()
        pg_pool.putconn(conn)

# Register the function to handle keymiss events
GB().foreach(fetch_data_on_miss).register(eventTypes=['keymiss'], mode='async_local')
