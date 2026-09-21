// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityDebugSettings } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';

export interface AdditionalDebugActionConfig<Action = (...args: unknown[]) => void> {
  action: Action;
  title: string;
}

export interface EntityDebugSettingPanelConfig {
  debugSettings: EntityDebugSettings;
  debugConfig: {
    maxDebugModeDuration?: number;
    additionalActionConfig?: AdditionalDebugActionConfig;
    entityType: EntityType;
  }
  onSettingsAppliedFn: (settings: EntityDebugSettings) => void;
}
