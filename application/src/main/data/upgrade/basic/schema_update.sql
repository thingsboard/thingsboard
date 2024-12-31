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

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS last_login_ts BIGINT;
UPDATE user_credentials c SET last_login_ts = (SELECT (additional_info::json ->> 'lastLoginTs')::bigint FROM tb_user u WHERE u.id = c.user_id)
  WHERE last_login_ts IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS failed_login_attempts INT;
UPDATE user_credentials c SET failed_login_attempts = (SELECT (additional_info::json ->> 'failedLoginAttempts')::int FROM tb_user u WHERE u.id = c.user_id)
  WHERE failed_login_attempts IS NULL;

UPDATE tb_user SET additional_info = (additional_info::jsonb - 'lastLoginTs' - 'failedLoginAttempts' - 'userCredentialsEnabled')::text
  WHERE additional_info IS NOT NULL AND additional_info != 'null' AND jsonb_typeof(additional_info::jsonb) = 'object';

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY START

ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS debug_settings varchar(1024) DEFAULT null;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rule_node' AND column_name = 'debug_mode')
            THEN
                UPDATE rule_node SET debug_settings = '{"failuresEnabled": true, "allEnabledUntil": ' || cast((extract(epoch from now()) + 900) * 1000 as bigint) || '}' WHERE debug_mode = true; -- 15 minutes according to thingsboard.yml default settings.
                ALTER TABLE rule_node DROP COLUMN debug_mode;
        END IF;
    END
$$;

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY END


-- CREATE MOBILE APP BUNDLES FROM EXISTING APPS

CREATE TABLE IF NOT EXISTS mobile_app_bundle (
    id uuid NOT NULL CONSTRAINT mobile_app_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    title varchar(255),
    description varchar(1024),
    android_app_id uuid UNIQUE,
    ios_app_id uuid UNIQUE,
    layout_config varchar(16384),
    oauth2_enabled boolean,
    CONSTRAINT fk_android_app_id FOREIGN KEY (android_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL,
    CONSTRAINT fk_ios_app_id FOREIGN KEY (ios_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS mobile_app_bundle_tenant_id ON mobile_app_bundle(tenant_id);

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS platform_type varchar(32),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS version_info varchar(100000),
    ADD COLUMN IF NOT EXISTS store_info varchar(16384),
    DROP CONSTRAINT IF EXISTS mobile_app_pkg_name_key,
    DROP CONSTRAINT IF EXISTS mobile_app_unq_key;

-- rename mobile_app_oauth2_client to mobile_app_bundle_oauth2_client
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'mobile_app_oauth2_client') THEN
            ALTER TABLE mobile_app_oauth2_client RENAME TO mobile_app_bundle_oauth2_client;
            ALTER TABLE mobile_app_bundle_oauth2_client DROP CONSTRAINT IF EXISTS fk_domain;
            ALTER TABLE mobile_app_bundle_oauth2_client RENAME COLUMN mobile_app_id TO mobile_app_bundle_id;
        END IF;
    END;
$$;


CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- duplicate each mobile app and create mobile app bundle for the pair of android and ios app
DO
$$
    DECLARE
        generatedBundleId uuid;
        iosAppId uuid;
        mobileAppRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'mobile_app' and column_name = 'oauth2_enabled') THEN
            UPDATE mobile_app SET platform_type = 'ANDROID' WHERE platform_type IS NULL;
            UPDATE mobile_app SET status = 'DRAFT' WHERE mobile_app.status IS NULL;
            FOR mobileAppRecord IN SELECT * FROM mobile_app
            LOOP
                -- duplicate app for iOS platform type
                iosAppId := uuid_generate_v4();
                INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                VALUES (iosAppId, mobileAppRecord.created_time, mobileAppRecord.tenant_id, mobileAppRecord.pkg_name, mobileAppRecord.app_secret, 'IOS', mobileAppRecord.status)
                ON CONFLICT DO NOTHING;
                -- create bundle for android and iOS app
                generatedBundleId := uuid_generate_v4();
                INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, ios_app_id, oauth2_enabled)
                VALUES (generatedBundleId, mobileAppRecord.created_time, mobileAppRecord.tenant_id,
                        mobileAppRecord.pkg_name || ' (autogenerated)', mobileAppRecord.id, iosAppId, mobileAppRecord.oauth2_enabled)
                ON CONFLICT DO NOTHING;
                UPDATE mobile_app_bundle_oauth2_client SET mobile_app_bundle_id = generatedBundleId WHERE mobile_app_bundle_id = mobileAppRecord.id;
            END LOOP;
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_mobile_app_bundle_oauth2_client_bundle_id') THEN
            ALTER TABLE mobile_app_bundle_oauth2_client ADD CONSTRAINT fk_mobile_app_bundle_oauth2_client_bundle_id
                FOREIGN KEY (mobile_app_bundle_id) REFERENCES mobile_app_bundle(id) ON DELETE CASCADE;
        END IF;
        ALTER TABLE mobile_app DROP COLUMN IF EXISTS oauth2_enabled;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'mobile_app_pkg_name_platform_unq_key') THEN
            ALTER TABLE mobile_app ADD CONSTRAINT mobile_app_pkg_name_platform_unq_key UNIQUE (pkg_name, platform_type);
        END IF;
    END;
$$;

ALTER TABLE IF EXISTS mobile_app_settings RENAME TO qr_code_settings;
ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS mobile_app_bundle_id uuid,
    ADD COLUMN IF NOT EXISTS android_enabled boolean,
    ADD COLUMN IF NOT EXISTS ios_enabled boolean;

-- migrate mobile apps from qr code settings to mobile_app, create mobile app bundle for the pair of apps
DO
$$
    DECLARE
        androidPkgName varchar;
        iosPkgName varchar;
        androidAppId uuid;
        iosAppId uuid;
        generatedBundleId uuid;
        qrCodeRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'qr_code_settings' AND column_name = 'android_config') THEN
            FOR qrCodeRecord IN SELECT * FROM qr_code_settings
            LOOP
                generatedBundleId := NULL;
                -- migrate android config
                IF (qrCodeRecord.android_config::jsonb ->> 'appPackage' IS NOT NULL) THEN
                    androidPkgName := qrCodeRecord.android_config::jsonb ->> 'appPackage';
                    SELECT id into androidAppId FROM mobile_app WHERE pkg_name = androidPkgName AND platform_type = 'ANDROID';
                    IF androidAppId IS NULL THEN
                        androidAppId := uuid_generate_v4();
                        INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                        VALUES (androidAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                                androidPkgName, 'ANDROID', 'DRAFT', qrCodeRecord.android_config::jsonb - 'appPackage' - 'enabled');
                        generatedBundleId := uuid_generate_v4();
                        INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id)
                        VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, androidPkgName || ' (autogenerated)', androidAppId);
                        UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId;
                    ELSE
                        UPDATE mobile_app SET store_info = qrCodeRecord.android_config::jsonb - 'appPackage' - 'enabled' WHERE id = androidAppId;
                        UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.android_app_id = androidAppId);
                    END IF;
                END IF;
                UPDATE qr_code_settings SET android_enabled = (qrCodeRecord.android_config::jsonb ->> 'enabled')::boolean WHERE id = qrCodeRecord.id;

                -- migrate ios config
                IF (qrCodeRecord.ios_config::jsonb ->> 'appId' IS NOT NULL) THEN
                    iosPkgName := substring(qrCodeRecord.ios_config::jsonb ->> 'appId', strpos(qrCodeRecord.ios_config::jsonb ->> 'appId', '.') + 1);
                    SELECT id INTO iosAppId FROM mobile_app WHERE pkg_name = iosPkgName AND platform_type = 'IOS';
                    IF iosAppId IS NULL THEN
                        iosAppId := uuid_generate_v4();
                        INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                        VALUES (iosAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                                iosPkgName, 'IOS', 'DRAFT', qrCodeRecord.ios_config::jsonb - 'enabled');
                        IF generatedBundleId IS NULL THEN
                            generatedBundleId := uuid_generate_v4();
                            INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id)
                            VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, iosPkgName || ' (autogenerated)', iosAppId);
                            UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId;
                        ELSE
                            UPDATE mobile_app_bundle SET ios_app_id = iosAppId WHERE id = generatedBundleId;
                        END IF;
                    ELSE
                        UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.ios_app_id = iosAppId);
                        UPDATE mobile_app SET store_info = qrCodeRecord.ios_config::jsonb - 'enabled' WHERE id = iosAppId;
                    END IF;
                END IF;
                UPDATE qr_code_settings SET ios_enabled = (qrCodeRecord.ios_config::jsonb -> 'enabled')::boolean WHERE id = qrCodeRecord.id;
            END LOOP;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_tenant_id_unq_key TO qr_code_settings_tenant_id_unq_key;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_pkey TO qr_code_settings_pkey;
        END IF;
        ALTER TABLE qr_code_settings DROP COLUMN IF EXISTS android_config, DROP COLUMN IF EXISTS ios_config;
    END;
$$;

-- update constraint name
DO
$$
    BEGIN
        ALTER TABLE domain DROP CONSTRAINT IF EXISTS domain_unq_key;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'domain_name_key') THEN
            ALTER TABLE domain ADD CONSTRAINT domain_name_key UNIQUE (name);
        END IF;
    END;
$$;

-- UPDATE RESOURCE JS_MODULE SUB TYPE START

UPDATE resource SET resource_sub_type = 'EXTENSION' WHERE resource_type = 'JS_MODULE' AND resource_sub_type IS NULL;

-- UPDATE RESOURCE JS_MODULE SUB TYPE END
