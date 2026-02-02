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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { QueueId } from '@shared/models/id/queue-id';
import { HasTenantId } from '@shared/models/entity.models';

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

export interface QueueStrategyData {
  label: string;
  hint: string;
}

export const QueueSubmitStrategyTypesMap = new Map<QueueSubmitStrategyTypes, QueueStrategyData>(
  [
    [QueueSubmitStrategyTypes.SEQUENTIAL_BY_ORIGINATOR, {
      label: 'queue.strategies.sequential-by-originator-label',
      hint: 'queue.strategies.sequential-by-originator-hint',
    }],
    [QueueSubmitStrategyTypes.SEQUENTIAL_BY_TENANT, {
      label: 'queue.strategies.sequential-by-tenant-label',
      hint: 'queue.strategies.sequential-by-tenant-hint',
    }],
    [QueueSubmitStrategyTypes.SEQUENTIAL, {
      label: 'queue.strategies.sequential-label',
      hint: 'queue.strategies.sequential-hint',
    }],
    [QueueSubmitStrategyTypes.BURST, {
      label: 'queue.strategies.burst-label',
      hint: 'queue.strategies.burst-hint',
    }],
    [QueueSubmitStrategyTypes.BATCH, {
      label: 'queue.strategies.batch-label',
      hint: 'queue.strategies.batch-hint',
    }]
  ]);

export enum QueueProcessingStrategyTypes {
  RETRY_FAILED_AND_TIMED_OUT = 'RETRY_FAILED_AND_TIMED_OUT',
  SKIP_ALL_FAILURES = 'SKIP_ALL_FAILURES',
  SKIP_ALL_FAILURES_AND_TIMED_OUT = 'SKIP_ALL_FAILURES_AND_TIMED_OUT',
  RETRY_ALL = 'RETRY_ALL',
  RETRY_FAILED = 'RETRY_FAILED',
  RETRY_TIMED_OUT = 'RETRY_TIMED_OUT'
}

export const QueueProcessingStrategyTypesMap = new Map<QueueProcessingStrategyTypes, QueueStrategyData>(
  [
    [QueueProcessingStrategyTypes.RETRY_FAILED_AND_TIMED_OUT, {
      label: 'queue.strategies.retry-failed-and-timeout-label',
      hint: 'queue.strategies.retry-failed-and-timeout-hint',
    }],
    [QueueProcessingStrategyTypes.SKIP_ALL_FAILURES, {
      label: 'queue.strategies.skip-all-failures-label',
      hint: 'queue.strategies.skip-all-failures-hint',
    }],
    [QueueProcessingStrategyTypes.SKIP_ALL_FAILURES_AND_TIMED_OUT, {
      label: 'queue.strategies.skip-all-failures-and-timeouts-label',
      hint: 'queue.strategies.skip-all-failures-and-timeouts-hint',
    }],
    [QueueProcessingStrategyTypes.RETRY_ALL, {
      label: 'queue.strategies.retry-all-label',
      hint: 'queue.strategies.retry-all-hint',
    }],
    [QueueProcessingStrategyTypes.RETRY_FAILED, {
      label: 'queue.strategies.retry-failed-label',
      hint: 'queue.strategies.retry-failed-hint',
    }],
    [QueueProcessingStrategyTypes.RETRY_TIMED_OUT, {
      label: 'queue.strategies.retry-timeout-label',
      hint: 'queue.strategies.retry-timeout-hint',
    }]
  ]);

export interface QueueInfo extends BaseData<QueueId>, HasTenantId {
  generatedId?: string;
  name: string;
  packProcessingTimeout: number;
  partitions: number;
  consumerPerPartition: boolean;
  pollInterval: number;
  processingStrategy: {
    type: QueueProcessingStrategyTypes;
    retries: number;
    failurePercentage: number;
    pauseBetweenRetries: number;
    maxPauseBetweenRetries: number;
  };
  submitStrategy: {
    type: QueueSubmitStrategyTypes;
    batchSize: number;
  };
  tenantId?: TenantId;
  topic: string;
  additionalInfo: {
    description?: string;
    customProperties?: string;
    duplicateMsgToAllPartitions?: boolean;
  };
}

export interface QueueStatisticsInfo extends Omit<BaseData<QueueId>, 'label'>, HasTenantId {
  queueName: string;
  serviceId: string;
}
