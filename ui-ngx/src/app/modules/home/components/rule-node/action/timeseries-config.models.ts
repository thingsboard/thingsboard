///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { DAY, SECOND } from '@shared/models/time/time.models';

export const maxDeduplicateTimeSecs = DAY / SECOND;

export interface TimeseriesNodeConfiguration {
  persistenceSettings: PersistenceSettings;
  defaultTTL: number;
  useServerTs: boolean;
}

export interface TimeseriesNodeConfigurationForm extends Omit<TimeseriesNodeConfiguration, 'persistenceSettings'> {
  persistenceSettings: PersistenceSettingsForm
}

export type PersistenceSettings = BasicPersistenceSettings & Partial<DeduplicatePersistenceStrategy> & Partial<AdvancedPersistenceStrategy>;

export type PersistenceSettingsForm = Omit<PersistenceSettings, keyof AdvancedPersistenceStrategy> & {
  isAdvanced: boolean;
  advanced?: Partial<AdvancedPersistenceStrategy>;
  type: PersistenceType;
};

export enum PersistenceType {
  ON_EVERY_MESSAGE = 'ON_EVERY_MESSAGE',
  DEDUPLICATE = 'DEDUPLICATE',
  WEBSOCKETS_ONLY = 'WEBSOCKETS_ONLY',
  ADVANCED = 'ADVANCED',
  SKIP = 'SKIP'
}

export const PersistenceTypeTranslationMap = new Map<PersistenceType, string>([
  [PersistenceType.ON_EVERY_MESSAGE, 'rule-node-config.save-time-series.strategy-type.every-message'],
  [PersistenceType.DEDUPLICATE, 'rule-node-config.save-time-series.strategy-type.deduplicate'],
  [PersistenceType.WEBSOCKETS_ONLY, 'rule-node-config.save-time-series.strategy-type.web-sockets-only'],
  [PersistenceType.SKIP, 'rule-node-config.save-time-series.strategy-type.skip'],
])

export interface BasicPersistenceSettings {
  type: PersistenceType;
}

export interface DeduplicatePersistenceStrategy extends BasicPersistenceSettings{
  deduplicationIntervalSecs: number;
}

export interface AdvancedPersistenceStrategy extends BasicPersistenceSettings{
  timeseries: AdvancedPersistenceConfig;
  latest: AdvancedPersistenceConfig;
  webSockets: AdvancedPersistenceConfig;
}

export type AdvancedPersistenceConfig = WithOptional<DeduplicatePersistenceStrategy, 'deduplicationIntervalSecs'>;

export const defaultAdvancedPersistenceConfig: AdvancedPersistenceConfig = {
  type: PersistenceType.ON_EVERY_MESSAGE
}

export const defaultAdvancedPersistenceStrategy: Omit<AdvancedPersistenceStrategy, 'type'> = {
  timeseries: defaultAdvancedPersistenceConfig,
  latest: defaultAdvancedPersistenceConfig,
  webSockets: defaultAdvancedPersistenceConfig,
}
