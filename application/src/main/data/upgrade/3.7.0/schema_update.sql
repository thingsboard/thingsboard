--
-- Copyright © 2016-2024 The Thingsboard Authors
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

-- UPDATE RESOURCE SUB TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'resource' AND column_name = 'resource_sub_type'
        ) THEN
            ALTER TABLE resource ADD COLUMN resource_sub_type varchar(32);
            UPDATE resource SET resource_sub_type = 'IMAGE' WHERE resource_type = 'IMAGE';
        END IF;
    END;
$$;

-- UPDATE RESOURCE SUB TYPE END

-- UPDATE WIDGETS BUNDLE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widgets_bundle' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widgets_bundle ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGETS BUNDLE END

-- UPDATE WIDGET TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widget_type' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widget_type ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGET TYPE END

-- KV VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS attribute_kv_version_seq cache 1;
CREATE SEQUENCE IF NOT EXISTS ts_kv_latest_version_seq cache 1;

ALTER TABLE attribute_kv ADD COLUMN IF NOT EXISTS version bigint default 0;
ALTER TABLE ts_kv_latest ADD COLUMN IF NOT EXISTS version bigint default 0;

-- KV VERSIONING UPDATE END

-- RELATION VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS relation_version_seq cache 1;
ALTER TABLE relation ADD COLUMN IF NOT EXISTS version bigint default 0;

-- RELATION VERSIONING UPDATE END


-- ENTITIES VERSIONING UPDATE START

ALTER TABLE device ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE device_profile ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE device_credentials ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE asset_profile ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE entity_view ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE edge ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE widget_type ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE widgets_bundle ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- ENTITIES VERSIONING UPDATE END

-- OAUTH2 UPDATE START

ALTER TABLE IF EXISTS oauth2_mobile RENAME TO mobile_app;
ALTER TABLE IF EXISTS oauth2_domain RENAME TO domain;
ALTER TABLE IF EXISTS oauth2_registration RENAME TO oauth2_client;

ALTER TABLE domain ADD COLUMN IF NOT EXISTS oauth2_enabled boolean,
                   ADD COLUMN IF NOT EXISTS edge_enabled boolean,
                   ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080',
                   DROP COLUMN IF EXISTS domain_scheme;

-- rename column domain_name to name
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='domain' and column_name='domain_name') THEN
            ALTER TABLE domain RENAME COLUMN domain_name TO name;
        END IF;
    END
$$;

-- delete duplicated domains
DELETE FROM domain d1 USING (
    SELECT MIN(ctid) as ctid, name
    FROM domain
    GROUP BY name HAVING COUNT(*) > 1
) d2 WHERE d1.name = d2.name AND d1.ctid <> d2.ctid;

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS oauth2_enabled boolean,
                       ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080';

-- delete duplicated apps
DELETE FROM mobile_app m1 USING (
    SELECT MIN(ctid) as ctid, pkg_name
    FROM mobile_app
    GROUP BY pkg_name HAVING COUNT(*) > 1
) m2 WHERE m1.pkg_name = m2.pkg_name AND m1.ctid <> m2.ctid;

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080',
                          ADD COLUMN IF NOT EXISTS title varchar(100);
UPDATE oauth2_client SET title = additional_info::jsonb->>'providerName' WHERE additional_info IS NOT NULL;

CREATE TABLE IF NOT EXISTS domain_oauth2_client (
    domain_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (domain_id) REFERENCES domain(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mobile_app_oauth2_client (
    mobile_app_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (mobile_app_id) REFERENCES mobile_app(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
);

-- migrate oauth2_params table
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'oauth2_params') THEN
            UPDATE domain SET oauth2_enabled = p.enabled,
                              edge_enabled = p.edge_enabled
            FROM oauth2_params p WHERE p.id = domain.oauth2_params_id;

            UPDATE mobile_app SET oauth2_enabled = p.enabled
            FROM oauth2_params p WHERE p.id = mobile_app.oauth2_params_id;

            INSERT INTO domain_oauth2_client(domain_id, oauth2_client_id)
                (SELECT d.id, r.id FROM domain d LEFT JOIN oauth2_client r on d.oauth2_params_id = r.oauth2_params_id
                 WHERE r.platforms IS NULL OR r.platforms IN ('','WEB'));

            INSERT INTO mobile_app_oauth2_client(mobile_app_id, oauth2_client_id)
                (SELECT m.id, r.id FROM mobile_app m LEFT JOIN oauth2_client r on m.oauth2_params_id = r.oauth2_params_id
                 WHERE r.platforms IS NULL OR r.platforms IN ('','ANDROID','IOS'));

            ALTER TABLE mobile_app RENAME CONSTRAINT oauth2_mobile_pkey TO mobile_app_pkey;
            ALTER TABLE domain RENAME CONSTRAINT oauth2_domain_pkey TO domain_pkey;
            ALTER TABLE oauth2_client RENAME CONSTRAINT oauth2_registration_pkey TO oauth2_client_pkey;

            ALTER TABLE domain DROP COLUMN oauth2_params_id;
            ALTER TABLE mobile_app DROP COLUMN oauth2_params_id;
            ALTER TABLE oauth2_client DROP COLUMN oauth2_params_id;

            ALTER TABLE mobile_app ADD CONSTRAINT mobile_app_unq_key UNIQUE (pkg_name);
            ALTER TABLE domain ADD CONSTRAINT domain_unq_key UNIQUE (name);

            DROP TABLE IF EXISTS oauth2_params;
            -- drop deprecated tables
            DROP TABLE IF EXISTS oauth2_client_registration_info;
            DROP TABLE IF EXISTS oauth2_client_registration;
        END IF;
    END
$$;

-- OAUTH2 UPDATE END

-- USER CREDENTIALS UPDATE START

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS activate_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing activation tokens
UPDATE user_credentials SET activate_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE activate_token IS NOT NULL AND activate_token_exp_time IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS reset_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing password reset tokens
UPDATE user_credentials SET reset_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE reset_token IS NOT NULL AND reset_token_exp_time IS NULL;

UPDATE admin_settings SET json_value = (json_value::jsonb || '{"userActivationTokenTtl":24,"passwordResetTokenTtl":24}'::jsonb)::varchar
    WHERE key = 'securitySettings';

-- USER CREDENTIALS UPDATE END
