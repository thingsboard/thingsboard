--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- Intentionally empty. The offline upgrade path is now fully bean-driven: every schema/data change lives in
-- an LtsMigration bean and its data/upgrade/lts/<version>/schema_update.sql (4.4 baseline DDL is in
-- V4_4_0_0Migration / lts/4.4.0.0/schema_update.sql). This file is kept present only so loadSql does not fail.
