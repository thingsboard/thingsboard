--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- 4.4 baseline flat DDL, applied by V4_4_0_0Migration during a 4.3.x -> 4.4 offline upgrade. Keep these
-- idempotent (ALTER ... IF NOT EXISTS): the offline path records the package version once at the end, so a
-- resumed upgrade re-runs this whole file.

-- RULE CHAIN NOTES MIGRATION START

ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);

-- RULE CHAIN NOTES MIGRATION END

-- DEVICE PROFILE SHARED X509 PROVISION CERTIFICATE START

DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'device_provision_key_unq_key') THEN
            ALTER TABLE device_profile DROP CONSTRAINT device_provision_key_unq_key;
        END IF;
    END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS device_provision_key_unq_key
    ON device_profile (provision_device_key,
                       COALESCE(profile_data -> 'provisionConfiguration' ->> 'certificateRegExPattern', ''));

-- DEVICE PROFILE SHARED X509 PROVISION CERTIFICATE END
