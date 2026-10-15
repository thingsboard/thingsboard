--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
--

CREATE
KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
	'class' : 'SimpleStrategy',
	'replication_factor' : 1
};
