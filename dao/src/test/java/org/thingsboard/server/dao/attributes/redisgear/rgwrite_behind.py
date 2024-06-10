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

# rgsync library test

from rgsync import RGWriteBehind, RGWriteThrough
from rgsync.Connectors import PostgresConnector, PostgresConnection

'''
Create Postgres connection object
'''
connection = PostgresConnection(os.getenv("PG_USER", "postgres"),
                                os.getenv("PG_PASSWORD", "postgres"),
                                os.getenv("PG_HOST", "localhost") + ':5432/' + os.getenv("PG_DBNAME", "postgres")
                                )

'''
Create Postgres persons connector
'''
personsConnector = PostgresConnector(connection, 'your_table', 'key')

personsMappings = {
    'value':'value'
    # 'last_name':'last',
    # 'age':'age'
}

RGWriteBehind(GB,  keysPrefix='attr', mappings=personsMappings, connector=personsConnector, name='PersonsWriteBehind',  version='99.99.99')

#RGWriteThrough(GB, keysPrefix='__',     mappings=personsMappings, connector=personsConnector, name='PersonsWriteThrough', version='99.99.99')