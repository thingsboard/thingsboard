--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- CALCULATED FIELD COMPUTE ON START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS compute_on varchar(32);

-- CALCULATED FIELD COMPUTE ON END
