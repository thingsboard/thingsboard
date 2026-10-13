// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData } from '@shared/models/base-data';
import { JobId } from '@shared/models/id/job-id';
import { TimePageLink } from '@shared/models/page/page-link';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { CalculatedFieldId } from '@shared/models/id/calculated-field-id';
import { Report } from '@shared/models/report.models';

export enum JobType {
  CF_REPROCESSING = 'CF_REPROCESSING',
  REPORT = 'REPORT'
}

export const jobTypeTranslations = new Map<JobType, string>(
  [
    [JobType.CF_REPROCESSING, 'task.task-type.calculated-field-reprocessing'],
    [JobType.REPORT, 'task.task-type.report']
  ]
)

export enum JobStatus {
  COMPLETED = 'COMPLETED',
  RUNNING = 'RUNNING',
  QUEUED = 'QUEUED',
  PENDING = 'PENDING',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export const jobStatusTranslations = new Map<JobStatus, string>(
  [
    [JobStatus.QUEUED, 'task.task-status.queued'],
    [JobStatus.PENDING, 'task.task-status.pending'],
    [JobStatus.RUNNING, 'task.task-status.running'],
    [JobStatus.COMPLETED, 'task.task-status.completed'],
    [JobStatus.FAILED, 'task.task-status.failed'],
    [JobStatus.CANCELLED, 'task.task-status.cancelled'],
  ]
);

export const workingTask = [JobStatus.QUEUED, JobStatus.PENDING, JobStatus.RUNNING];

export interface BasicTaskResult {
  key: string;
  success: boolean;
  discarded: boolean;
  jobType: JobType;
}

export interface BasicTaskFailure {
  error: string;
}

export interface CfReprocessingTaskFailure extends BasicTaskFailure{
  entityInfo?: EntityInfoData;
}

export interface CfReprocessingTaskResult extends BasicTaskResult {
  failure: CfReprocessingTaskFailure;
}

export type TaskResult = CfReprocessingTaskResult & BasicTaskFailure;

export interface BasicJobConfiguration {
  tasksKey: string;
  toReprocess: TaskResult[];
  type: JobType;
}

export interface CfReprocessingJobConfiguration extends BasicJobConfiguration {
  calculatedFieldId: CalculatedFieldId;
  startTs: number;
  endTs: number;
}

export type JobConfiguration = CfReprocessingJobConfiguration;

export interface BasicJobResult {
  successfulCount: number;
  failedCount: number;
  discardedCount: number;
  totalCount?: number;
  jobType?: JobType;
  results: TaskResult[];
  generalError?: string;
  startTs?: number;
  finishTs?: number;
  cancellationTs?: number;
}

export interface ReportJobResult {
  report: Report;
}

export type JobResult = BasicJobResult & ReportJobResult;

export interface Job extends Omit<BaseData<JobId>, 'label' | 'ownerId' | 'customerId' | 'name'> {
  type: JobType;
  key: string;
  status: JobStatus;
  configuration: JobConfiguration;
  result: JobResult;
  entityName: string;
  entityId: EntityId;
}

export interface JobFilter {
  startTs?: number;
  endTs?: number;
  timeWindow?: number;
  types?: Array<JobType>;
  statuses?: Array<JobStatus>;
  entities?: Array<EntityId>;
  includeCustomers?: boolean;
}

export interface TaskManagerConfig extends EntityTableConfig<Job, TimePageLink> {
  componentData?: {
    filter?: JobFilter;
  }
}

export class JobQuery {

  pageLink: TimePageLink;
  types: JobType[];
  statuses: JobStatus[];
  entities: EntityId[];
  includeCustomers: boolean;

  constructor(pageLink: TimePageLink,
              jobFilter: JobFilter,
              includeCustomers = false) {
    this.pageLink = pageLink;
    this.types = jobFilter.types;
    this.statuses = jobFilter.statuses;
    this.entities = jobFilter.entities;
    this.includeCustomers = includeCustomers;
  }

  public toQuery(): string {
    let query = this.pageLink.toQuery();
    if (this.types?.length) {
      query += `&types=${this.types.map(type => encodeURIComponent(type)).join(',')}`;
    }
    if (this.statuses?.length) {
      query += `&statuses=${this.statuses.join(',')}`;
    }
    if (this.entities?.length) {
      query += `&entities=${this.entities.map(id => id.id).join(',')}`;
    }
    if (this.includeCustomers) {
      query += `&includeCustomers=true`;
    }
    return query;
  }
}

export const processTask = (result: JobResult): number => {
  let progress = 0;
  if (result.discardedCount) {
    progress += result.discardedCount;
  }
  if (result.failedCount) {
    progress += result.failedCount;
  }
  if (result.successfulCount) {
    progress += result.successfulCount;
  }
  return progress;
}
