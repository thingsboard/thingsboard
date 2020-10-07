///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { BaseData, HasId } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';

export enum ServiceType {
  TB_CORE = 'TB_CORE',
  TB_RULE_ENGINE = 'TB_RULE_ENGINE',
  TB_TRANSPORT = 'TB_TRANSPORT',
  JS_EXECUTOR = 'JS_EXECUTOR'
}

export enum QueueSubmitStrategyTypes {
  SEQUENTIAL_BY_ORIGINATOR = 'SEQUENTIAL_BY_ORIGINATOR',
  SEQUENTIAL_BY_TENANT = 'SEQUENTIAL_BY_TENANT',
  SEQUENTIAL = 'SEQUENTIAL',
  BURST = 'BURST',
  BATCH = 'BATCH'
}

export enum QueueProcessingStrategyTypes {
  RETRY_FAILED_AND_TIMED_OUT = 'RETRY_FAILED_AND_TIMED_OUT',
  SKIP_ALL_FAILURES = 'SKIP_ALL_FAILURES',
  RETRY_ALL = 'RETRY_ALL',
  RETRY_FAILED = 'RETRY_FAILED',
  RETRY_TIMED_OUT = 'RETRY_TIMED_OUT'
}

export interface QueueInfo extends BaseData<HasId> {
  packProcessingTimeout: number;
  partitions: number;
  pollInterval: number;
  processingStrategy: {
    type: QueueProcessingStrategyTypes,
    retries: number,
    failurePercentage: number,
    pauseBetweenRetries: number,
    maxPauseBetweenRetries: number
  };
  submitStrategy: {
    type: QueueSubmitStrategyTypes,
    batchSize: number,
  };
  tenantId: TenantId;
  topic: string;
}

