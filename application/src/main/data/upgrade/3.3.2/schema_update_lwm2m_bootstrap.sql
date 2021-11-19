--
-- Copyright © 2016-2021 The Thingsboard Authors
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

CREATE OR REPLACE FUNCTION get_bootstrap_3_3_3(bootstrap_in jsonb, publickey_bs text, publickey_lw text) RETURNS jsonb AS
$$
BEGIN
  RETURN json_build_array(
    json_build_object('shortServerId', bootstrap_in::json #> '{bootstrapServer}' -> 'serverId',
                      'securityMode', bootstrap_in::json #> '{bootstrapServer}' ->> 'securityMode',
                      'binding', bootstrap_in::json #> '{servers}' ->> 'binding',
                      'lifetime', bootstrap_in::json #> '{servers}' -> 'lifetime',
                      'notifIfDisabled', bootstrap_in::json #> '{servers}' -> 'notifIfDisabled',
                      'defaultMinPeriod', bootstrap_in::json #> '{servers}' -> 'defaultMinPeriod',
                      'host', bootstrap_in::json #> '{bootstrapServer}' ->> 'host',
                      'port', bootstrap_in::json #> '{bootstrapServer}' -> 'port',
                      'serverPublicKey', publickey_bs,
                      'bootstrapServerIs', true,
                      'clientHoldOffTime', bootstrap_in::json #> '{bootstrapServer}' -> 'clientHoldOffTime',
                      'bootstrapServerAccountTimeout',
                      bootstrap_in::json #> '{bootstrapServer}' -> 'bootstrapServerAccountTimeout'
      ),
    json_build_object('shortServerId', bootstrap_in::json #> '{lwm2mServer}' -> 'serverId',
                      'securityMode', bootstrap_in::json #> '{lwm2mServer}' ->> 'securityMode',
                      'binding', bootstrap_in::json #> '{servers}' ->> 'binding',
                      'lifetime', bootstrap_in::json #> '{servers}' -> 'lifetime',
                      'notifIfDisabled', bootstrap_in::json #> '{servers}' -> 'notifIfDisabled',
                      'defaultMinPeriod', bootstrap_in::json #> '{servers}' -> 'defaultMinPeriod',
                      'host', bootstrap_in::json #> '{lwm2mServer}' ->> 'host',
                      'port', bootstrap_in::json #> '{lwm2mServer}' -> 'port',
                      'serverPublicKey', publickey_lw,
                      'bootstrapServerIs', false,
                      'clientHoldOffTime', bootstrap_in::json #> '{lwm2mServer}' -> 'clientHoldOffTime',
                      'bootstrapServerAccountTimeout',
                      bootstrap_in::json #> '{lwm2mServer}' -> 'bootstrapServerAccountTimeout'
      )
    );
END ;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE update_profile_bootstrap()
  LANGUAGE plpgsql AS
$$

BEGIN

  UPDATE device_profile
  SET profile_data = jsonb_set(
    profile_data,
    '{transportConfiguration, bootstrap}',
    get_bootstrap_3_3_3(
      profile_data::jsonb #> '{transportConfiguration,bootstrap}',
      subquery.publickey_bs,
      subquery.publickey_lw),
    true)
  FROM (
         SELECT id,
                encode(
                  decode(profile_data::json #> '{transportConfiguration,bootstrap,bootstrapServer}' ->>
                         'serverPublicKey', 'hex')::bytea, 'base64') AS publickey_bs,
                encode(
                  decode(profile_data::json #> '{transportConfiguration,bootstrap,lwm2mServer}' ->>
                         'serverPublicKey', 'hex')::bytea, 'base64') AS publickey_lw
         FROM device_profile
         WHERE transport_type = 'LWM2M'
       ) AS subquery
  WHERE device_profile.id = subquery.id;

END;
$$;
