--
-- Copyright © 2016-2020 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE OR REPLACE FUNCTION extract_ts(uuid UUID) RETURNS BIGINT AS
$$
DECLARE
    bytes bytea;
BEGIN
    bytes := uuid_send(uuid);
    RETURN
                (
                            (
                                            (get_byte(bytes, 0)::bigint << 24) |
                                            (get_byte(bytes, 1)::bigint << 16) |
                                            (get_byte(bytes, 2)::bigint <<  8) |
                                            (get_byte(bytes, 3)::bigint <<  0)
                                ) + (
                                    ((get_byte(bytes, 4)::bigint << 8 |
                                      get_byte(bytes, 5)::bigint)) << 32
                                ) + (
                                    (((get_byte(bytes, 6)::bigint & 15) << 8 | get_byte(bytes, 7)::bigint) & 4095) << 48
                                ) - 122192928000000000
                    ) / 10000::double precision
        ;
END
$$ LANGUAGE plpgsql
    IMMUTABLE PARALLEL SAFE
    RETURNS NULL ON NULL INPUT;
