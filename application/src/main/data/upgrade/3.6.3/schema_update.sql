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

-- UPDATE PUBLIC CUSTOMERS START

ALTER TABLE customer ADD COLUMN IF NOT EXISTS is_public boolean DEFAULT false;
UPDATE customer SET is_public = true WHERE title = 'Public';

-- UPDATE PUBLIC CUSTOMERS END

-- UPDATE CUSTOMERS WITH SAME TITLE START

-- Check for postgres version to install pgcrypto
-- if version less then 130000 to have ability use gen_random_uuid();

DO
$$
    BEGIN
        IF (SELECT current_setting('server_version_num')::int) < 130000 THEN
            CREATE EXTENSION IF NOT EXISTS pgcrypto;
        END IF;
    END
$$;

CREATE OR REPLACE PROCEDURE update_customers_with_the_same_title()
    LANGUAGE plpgsql
AS
$$
DECLARE
    customer_record  RECORD;
    dashboard_record RECORD;
    new_title        TEXT;
    updated_json     JSONB;
BEGIN
    RAISE NOTICE 'Starting the customer and dashboard update process.';

    FOR customer_record IN
        SELECT id, tenant_id, title
        FROM customer
        WHERE id IN (SELECT c1.id
                     FROM customer c1
                              JOIN customer c2 ON c1.tenant_id = c2.tenant_id AND c1.title = c2.title
                     WHERE c1.id > c2.id)
        LOOP
            new_title := customer_record.title || '_' || gen_random_uuid();
            UPDATE customer
            SET title = new_title
            WHERE id = customer_record.id;
            RAISE NOTICE 'Updated customer with id: % with new title: %', customer_record.id, new_title;

            -- Find and update related dashboards for the customer
            FOR dashboard_record IN
                SELECT d.id, d.assigned_customers
                FROM dashboard d
                         JOIN relation r ON d.id = r.to_id
                WHERE r.from_id = customer_record.id
                  AND r.to_type = 'DASHBOARD'
                  AND r.relation_type_group = 'DASHBOARD'
                  AND r.relation_type = 'Contains'
                LOOP
                    -- Update each assigned_customers entry where the customerId matches
                    updated_json := (SELECT jsonb_agg(
                                                    CASE
                                                        WHEN (value -> 'customerId' ->> 'id')::uuid = customer_record.id
                                                            THEN jsonb_set(value, '{title}', ('"' || new_title || '"')::jsonb)
                                                        ELSE value
                                                        END
                                                )
                                     FROM jsonb_array_elements(dashboard_record.assigned_customers::jsonb));

                    UPDATE dashboard
                    SET assigned_customers = updated_json
                    WHERE id = dashboard_record.id;
                    RAISE NOTICE 'Updated dashboard with id: % with new assigned_customers: %', dashboard_record.id, updated_json;
                END LOOP;
        END LOOP;
    RAISE NOTICE 'Customers and dashboards update process completed successfully!';
END;
$$;

call update_customers_with_the_same_title();

DROP PROCEDURE IF EXISTS update_customers_with_the_same_title;

-- UPDATE CUSTOMERS WITH SAME TITLE END

-- CUSTOMER UNIQUE CONSTRAINT UPDATE START

ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_title_unq_key;
ALTER TABLE customer ADD CONSTRAINT customer_title_unq_key UNIQUE (tenant_id, title);

-- CUSTOMER UNIQUE CONSTRAINT UPDATE END
