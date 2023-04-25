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

-- MAIL CONFIG TEMPLATE START;

CREATE TABLE IF NOT EXISTS mail_config_template (
   id uuid NOT NULL CONSTRAINT mail_config_template_pkey PRIMARY KEY,
   created_time bigint NOT NULL,
   provider_id varchar(255),
   smtp_protocol varchar(255),
   smtp_host varchar(255),
   smtp_port int,
   timeout int,
   tls_enabled boolean,
   tls_version varchar(255),
   authorization_uri varchar(255),
   token_uri varchar(255),
   scope varchar(255),
   help_link varchar(255),
   CONSTRAINT mail_config_template_provider_id_unq_key UNIQUE (provider_id)
);

-- MAIL CONFIG TEMPLATE END;