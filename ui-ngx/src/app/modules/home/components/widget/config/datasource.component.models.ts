// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityAliasSelectCallbacks } from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';
import { FilterSelectCallbacks } from '@home/components/widget/lib/settings/common/filter/filter-select.component.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';

export type DatasourceCallbacks = EntityAliasSelectCallbacks & FilterSelectCallbacks & DataKeysCallbacks;
