// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ScriptLanguage, TelemetryType } from '@shared/public-api';
import { AggMathFunction } from '@home/components/rule-node/rule-node-config.models';

export interface AggLatestMappingFilter {
  clientAttributeNames: string[];
  sharedAttributeNames: string[];
  serverAttributeNames: string[];
  latestTsKeyNames: string[];
  scriptLang: ScriptLanguage;
  filterFunction: string;
  tbelFilterFunction: string;
}

export interface AggLatestMapping {
  source: string;
  sourceScope: TelemetryType;
  defaultValue: number;
  target: string;
  aggFunction: AggMathFunction;
  filter?: AggLatestMappingFilter;
}
