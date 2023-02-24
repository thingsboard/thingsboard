--
-- Copyright © 2016-2023 The Thingsboard Authors
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


CREATE OR REPLACE PROCEDURE update_profile_bootstrap()
  LANGUAGE plpgsql AS
$$

BEGIN

  UPDATE device_profile
  SET profile_data = jsonb_set(
    profile_data,
    '{transportConfiguration}',
    get_bootstrap(
      profile_data::jsonb #> '{transportConfiguration}',
      subquery.publickey_bs,
      subquery.publickey_lw,
      profile_data::json #>> '{transportConfiguration, bootstrap, bootstrapServer, securityMode}',
      profile_data::json #>> '{transportConfiguration, bootstrap, lwm2mServer, securityMode}'),
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
  WHERE device_profile.id = subquery.id
    AND subquery.publickey_bs IS NOT NULL
    AND subquery.publickey_lw IS NOT NULL;

END;
$$;

CREATE OR REPLACE FUNCTION get_bootstrap(transport_configuration_in jsonb, publickey_bs text,
                                         publickey_lw text, security_mode_bs text,
                                         security_mode_lw text) RETURNS jsonb AS
$$

DECLARE
  bootstrap_new jsonb;
  bootstrap_in  jsonb;

BEGIN

  IF security_mode_lw IS NULL THEN
    security_mode_lw := 'NO_SEC';
  END IF;

  IF security_mode_bs IS NULL THEN
    security_mode_bs := 'NO_SEC';
  END IF;

  bootstrap_in := transport_configuration_in::jsonb #> '{bootstrap}';
  bootstrap_new := json_build_array(
    json_build_object('shortServerId', bootstrap_in::json #> '{bootstrapServer}' -> 'serverId',
                      'securityMode', security_mode_bs,
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
                      'securityMode', security_mode_lw,
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
  RETURN jsonb_set(
           transport_configuration_in,
           '{bootstrap}',
           bootstrap_new,
           true) || '{"bootstrapServerUpdateEnable": true}';

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE update_device_credentials_to_base64_and_bootstrap()
  LANGUAGE plpgsql AS
$$

BEGIN

  UPDATE device_credentials
  SET credentials_value = get_device_and_bootstrap(credentials_value::text)
  WHERE credentials_type = 'LWM2M_CREDENTIALS';
END;
$$;

CREATE OR REPLACE FUNCTION get_device_and_bootstrap(IN credentials_value text, OUT credentials_value_new text)
  LANGUAGE plpgsql AS
$$
DECLARE
  client_secret_key                    text;
  client_public_key_or_id              text;
  client_key_value_object              jsonb;
  client_bootstrap_server_value_object jsonb;
  client_bootstrap_server_object       jsonb;
  client_bootstrap_object              jsonb;

BEGIN
  credentials_value_new := credentials_value;
  IF credentials_value::jsonb #> '{client}' ->> 'securityConfigClientMode' = 'RPK' AND
     NULLIF((credentials_value::jsonb #> '{client}' ->> 'key' ~ '^[0-9a-fA-F]+$')::text, 'false') = 'true' THEN
    client_public_key_or_id := encode(decode(credentials_value::jsonb #> '{client}' ->> 'key', 'hex')::bytea, 'base64');
    client_key_value_object := json_build_object(
      'endpoint', credentials_value::jsonb #> '{client}' ->> 'endpoint',
      'securityConfigClientMode', credentials_value::jsonb #> '{client}' ->> 'securityConfigClientMode',
      'key', client_public_key_or_id);
    credentials_value_new :=
        credentials_value_new::jsonb || json_build_object('client', client_key_value_object)::jsonb;
  END IF;
  IF credentials_value::jsonb #> '{client}' ->> 'securityConfigClientMode' = 'X509' AND
     NULLIF((credentials_value::jsonb #> '{client}' ->> 'cert' ~ '^[0-9a-fA-F]+$')::text, 'false') = 'true' THEN
    client_public_key_or_id :=
      encode(decode(credentials_value::jsonb #> '{client}' ->> 'cert', 'hex')::bytea, 'base64');
    client_key_value_object := json_build_object(
      'endpoint', credentials_value::jsonb #> '{client}' ->> 'endpoint',
      'securityConfigClientMode', credentials_value::jsonb #> '{client}' ->> 'securityConfigClientMode',
      'cert', client_public_key_or_id);
    credentials_value_new :=
        credentials_value_new::jsonb || json_build_object('client', client_key_value_object)::jsonb;
  END IF;

  IF credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'securityMode' = 'RPK' OR
     credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'securityMode' = 'X509' THEN
    IF NULLIF((credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'clientSecretKey' ~ '^[0-9a-fA-F]+$')::text,
              'false') = 'true' AND
       NULLIF(
         (credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'clientPublicKeyOrId' ~ '^[0-9a-fA-F]+$')::text,
         'false') = 'true' THEN
      client_secret_key :=
        encode(decode(credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'clientSecretKey', 'hex')::bytea,
               'base64');
      client_public_key_or_id := encode(
        decode(credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'clientPublicKeyOrId', 'hex')::bytea,
        'base64');
      client_bootstrap_server_value_object := jsonb_build_object(
        'securityMode', credentials_value::jsonb #> '{bootstrap,lwm2mServer}' ->> 'securityMode',
        'clientPublicKeyOrId', client_public_key_or_id,
        'clientSecretKey', client_secret_key
        );
      client_bootstrap_server_object := jsonb_build_object('lwm2mServer', client_bootstrap_server_value_object::jsonb);
      client_bootstrap_object := credentials_value_new::jsonb #> '{bootstrap}' || client_bootstrap_server_object::jsonb;
      credentials_value_new :=
        jsonb_set(credentials_value_new::jsonb, '{bootstrap}', client_bootstrap_object::jsonb, false)::jsonb;
    END IF;
  END IF;

  IF credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'securityMode' = 'RPK' OR
     credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'securityMode' = 'X509' THEN
    IF NULLIF(
         (credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'clientSecretKey' ~ '^[0-9a-fA-F]+$')::text,
         'false') = 'true' AND
       NULLIF(
         (credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'clientPublicKeyOrId' ~ '^[0-9a-fA-F]+$')::text,
         'false') = 'true' THEN
      client_secret_key :=
        encode(
          decode(credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'clientSecretKey', 'hex')::bytea,
          'base64');
      client_public_key_or_id := encode(
        decode(credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'clientPublicKeyOrId', 'hex')::bytea,
        'base64');
      client_bootstrap_server_value_object := jsonb_build_object(
        'securityMode', credentials_value::jsonb #> '{bootstrap,bootstrapServer}' ->> 'securityMode',
        'clientPublicKeyOrId', client_public_key_or_id,
        'clientSecretKey', client_secret_key
        );
      client_bootstrap_server_object :=
        jsonb_build_object('bootstrapServer', client_bootstrap_server_value_object::jsonb);
      client_bootstrap_object := credentials_value_new::jsonb #> '{bootstrap}' || client_bootstrap_server_object::jsonb;
      credentials_value_new :=
        jsonb_set(credentials_value_new::jsonb, '{bootstrap}', client_bootstrap_object::jsonb, false)::jsonb;
    END IF;
  END IF;

END;
$$;