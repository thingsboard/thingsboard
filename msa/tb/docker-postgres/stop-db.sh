#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)

${PG_CTL} stop
