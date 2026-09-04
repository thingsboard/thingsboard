// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { DAY, SECOND } from '@shared/models/time/time.models';

export const maxDeduplicateTimeSecs = DAY / SECOND;

export interface TimeseriesNodeConfiguration {
  processingSettings: ProcessingSettings;
  defaultTTL: number;
  useServerTs: boolean;
}

export interface TimeseriesNodeConfigurationForm extends Omit<TimeseriesNodeConfiguration, 'processingSettings'> {
  processingSettings: ProcessingSettingsForm
}

export type ProcessingSettings = BasicProcessingSettings & Partial<DeduplicateProcessingStrategy> & Partial<AdvancedProcessingStrategy>;

export type ProcessingSettingsForm = Omit<ProcessingSettings, keyof AdvancedProcessingStrategy> & {
  isAdvanced: boolean;
  advanced?: Partial<AdvancedProcessingStrategy>;
  type: ProcessingType;
};

export enum ProcessingType {
  ON_EVERY_MESSAGE = 'ON_EVERY_MESSAGE',
  DEDUPLICATE = 'DEDUPLICATE',
  WEBSOCKETS_ONLY = 'WEBSOCKETS_ONLY',
  ADVANCED = 'ADVANCED',
  SKIP = 'SKIP'
}

export const ProcessingTypeTranslationMap = new Map<ProcessingType, string>([
  [ProcessingType.ON_EVERY_MESSAGE, 'rule-node-config.save-time-series.strategy-type.every-message'],
  [ProcessingType.DEDUPLICATE, 'rule-node-config.save-time-series.strategy-type.deduplicate'],
  [ProcessingType.WEBSOCKETS_ONLY, 'rule-node-config.save-time-series.strategy-type.web-sockets-only'],
  [ProcessingType.SKIP, 'rule-node-config.save-time-series.strategy-type.skip'],
])

export interface BasicProcessingSettings {
  type: ProcessingType;
}

export interface DeduplicateProcessingStrategy extends BasicProcessingSettings {
  deduplicationIntervalSecs: number;
}

export interface AdvancedProcessingStrategy extends BasicProcessingSettings {
  timeseries: AdvancedProcessingConfig;
  latest: AdvancedProcessingConfig;
  webSockets: AdvancedProcessingConfig;
  calculatedFields: AdvancedProcessingConfig;
}

export type AdvancedProcessingConfig = WithOptional<DeduplicateProcessingStrategy, 'deduplicationIntervalSecs'>;

export const defaultAdvancedProcessingConfig: AdvancedProcessingConfig = {
  type: ProcessingType.ON_EVERY_MESSAGE
}

export const defaultAdvancedProcessingStrategy: Omit<AdvancedProcessingStrategy, 'type'> = {
  timeseries: defaultAdvancedProcessingConfig,
  latest: defaultAdvancedProcessingConfig,
  webSockets: defaultAdvancedProcessingConfig,
  calculatedFields: defaultAdvancedProcessingConfig,
}
