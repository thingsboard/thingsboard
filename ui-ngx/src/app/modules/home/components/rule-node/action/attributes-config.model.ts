///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { BasicProcessingSettings, ProcessingType } from '@home/components/rule-node/action/timeseries-config.models';

export interface AttributeNodeConfiguration {
  processingSettings: AttributeProcessingSettings;
  scope: AttributeScope;
  notifyDevice: boolean;
  sendAttributesUpdatedNotification: boolean;
  updateAttributesOnlyOnValueChange: boolean;
}

export interface AttributeNodeConfigurationForm extends Omit<AttributeNodeConfiguration, 'processingSettings'> {
  processingSettings: AttributeProcessingSettingsForm
}

export type AttributeProcessingSettings = BasicProcessingSettings & Partial<AttributeDeduplicateProcessingStrategy> & Partial<AttributeAdvancedProcessingStrategy>;

export type AttributeProcessingSettingsForm = Omit<AttributeProcessingSettings, keyof AttributeAdvancedProcessingStrategy> & {
  isAdvanced: boolean;
  advanced?: Partial<AttributeAdvancedProcessingStrategy>;
  type: ProcessingType;
};

export interface AttributeDeduplicateProcessingStrategy extends BasicProcessingSettings {
  deduplicationIntervalSecs: number;
}

export interface AttributeAdvancedProcessingStrategy extends BasicProcessingSettings {
  attributes: AttributeAdvancedProcessingConfig;
  webSockets: AttributeAdvancedProcessingConfig;
  calculatedFields: AttributeAdvancedProcessingConfig;
}

export type AttributeAdvancedProcessingConfig = WithOptional<AttributeDeduplicateProcessingStrategy, 'deduplicationIntervalSecs'>;

export const defaultAdvancedProcessingConfig: AttributeAdvancedProcessingConfig = {
  type: ProcessingType.ON_EVERY_MESSAGE
}

export const defaultAttributeAdvancedProcessingStrategy: Omit<AttributeAdvancedProcessingStrategy, 'type'> = {
  attributes: defaultAdvancedProcessingConfig,
  webSockets: defaultAdvancedProcessingConfig,
  calculatedFields: defaultAdvancedProcessingConfig,
}
