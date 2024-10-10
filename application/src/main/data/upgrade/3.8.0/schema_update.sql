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
    CONSTRAINT fk_android_app_id FOREIGN KEY (android_app_id) REFERENCES mobile_app(id),
    CONSTRAINT fk_ios_app_id FOREIGN KEY (ios_app_id) REFERENCES mobile_app(id)
);

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS platform_type varchar(32),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS version_info varchar(16384),
    ADD COLUMN IF NOT EXISTS qr_code_config varchar(16384),
    DROP CONSTRAINT IF EXISTS mobile_app_pkg_name_key;

-- rename mobile_app_oauth2_client to mobile_app_bundle_oauth2_client
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT * FROM information_schema.tables WHERE table_name = 'mobile_app_oauth2_client') THEN
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
        IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'mobile_app' and column_name = 'oauth2_enabled') THEN
            UPDATE mobile_app SET platform_type = 'ANDROID' WHERE platform_type IS NULL;
            UPDATE mobile_app SET status = 'PUBLISHED' WHERE mobile_app.status IS NULL;
            FOR mobileAppRecord IN SELECT * FROM mobile_app
            LOOP
                -- duplicate app for iOS platform type
                iosAppId := uuid_generate_v4();
                INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                VALUES (iosAppId, (extract(epoch from now()) * 1000), mobileAppRecord.tenant_id, mobileAppRecord.pkg_name, mobileAppRecord.app_secret, 'IOS', mobileAppRecord.status);
                -- create bundle for android and iOS app
                generatedBundleId := uuid_generate_v4();
                INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, ios_app_id, oauth2_enabled)
                    VALUES (generatedBundleId, (extract(epoch from now()) * 1000), mobileAppRecord.tenant_id,
                            'App bundle ' || mobileAppRecord.pkg_name, mobileAppRecord.id, iosAppId, mobileAppRecord.oauth2_enabled);
                UPDATE mobile_app_bundle_oauth2_client SET mobile_app_bundle_id = generatedBundleId WHERE mobile_app_bundle_id = mobileAppRecord.id;
            END LOOP;
        END IF;
        ALTER TABLE mobile_app DROP COLUMN IF EXISTS oauth2_enabled;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'pkg_platform_unique') THEN
            ALTER TABLE mobile_app ADD CONSTRAINT pkg_platform_unique UNIQUE (pkg_name, platform_type);
        END IF;
    END;
$$;

ALTER TABLE IF EXISTS mobile_app_settings RENAME TO qr_code_settings;
ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS mobile_app_bundle_id uuid;

-- migrate mobile apps from qr code settings to mobile_app, create mobile app bundle for the pair of apps
DO
$$
    DECLARE
        iosPkgName varchar;
        androidAppId uuid;
        iosAppId uuid;
        generatedBundleId uuid;
        qrCodeRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'qr_code_settings' and column_name = 'android_config') THEN
            FOR qrCodeRecord IN SELECT * FROM qr_code_settings
            LOOP
                generatedBundleId := NULL;
                -- migrate android config
                SELECT id into androidAppId FROM mobile_app WHERE pkg_name = qrCodeRecord.android_config::jsonb ->> 'appPackage' AND platform_type = 'ANDROID';
                IF androidAppId IS NULL THEN
                    androidAppId := uuid_generate_v4();
                    INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, qr_code_config)
                    VALUES (androidAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                            qrCodeRecord.android_config::jsonb ->> 'appPackage', 'ANDROID', 'PUBLISHED', qrCodeRecord.android_config::jsonb || '{"type": "ANDROID"}'::jsonb);
                    generatedBundleId := uuid_generate_v4();
                    INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id)
                    VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, 'App bundle for qr code', androidAppId);
                    UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                ELSE
                    UPDATE mobile_app SET qr_code_config = qrCodeRecord.android_config::jsonb || '{"type": "ANDROID"}'::jsonb WHERE id = androidAppId;
                    UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.android_app_id = androidAppId) WHERE id = qrCodeRecord.id;
                END IF;

                -- migrate ios config
                iosPkgName := substring(qrCodeRecord.ios_config::jsonb ->> 'appId', strpos(qrCodeRecord.ios_config::jsonb ->> 'appId', '.') + 1);
                SELECT id into iosAppId FROM mobile_app WHERE pkg_name = iosPkgName AND platform_type = 'IOS';
                IF iosAppId IS NULL THEN
                    iosAppId := uuid_generate_v4();
                    INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, qr_code_config)
                    VALUES (iosAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                            iosPkgName, 'IOS', 'PUBLISHED', qrCodeRecord.ios_config::jsonb || '{"type": "IOS"}'::jsonb);
                    IF generatedBundleId IS NULL THEN
                        generatedBundleId := uuid_generate_v4();
                        INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id)
                        VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, 'App bundle for qr code', iosAppId);
                        UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                    ELSE
                        UPDATE mobile_app_bundle SET ios_app_id = iosAppId WHERE id = generatedBundleId;
                    END IF;
                ELSE
                    UPDATE mobile_app SET qr_code_config = qrCodeRecord.ios_config::jsonb || '{"type": "IOS"}'::jsonb WHERE id = iosAppId;
                    UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.ios_app_id = iosAppId) WHERE id = qrCodeRecord.id;
                END IF;
            END LOOP;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_tenant_id_unq_key TO qr_code_settings_tenant_id_unq_key;
        END IF;
        ALTER TABLE qr_code_settings DROP COLUMN IF EXISTS android_config, DROP COLUMN IF EXISTS ios_config;
    END;
$$;
