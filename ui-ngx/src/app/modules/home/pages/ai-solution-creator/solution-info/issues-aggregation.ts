// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { EntityResult } from '@shared/models/solution-creator.models';
import { TranslateService } from '@ngx-translate/core';

export enum IssueStatus {
  WARN = 'WARN',
  ERROR = 'ERROR',
  OK = 'OK'
}

export interface AggregatedEntityResult {
  /** Grouping key — `${entityType}:${additionalData.name}` so popovers don't collide across types */
  key: string;
  /** Representative entry chosen for primary card/row info; prefers an OK entry to keep id/userId */
  primary: EntityResult;
  /** Worst-case status across the group */
  status: IssueStatus;
  /** Entries with status === 'ERROR' */
  errors: EntityResult[];
  /** Entries with status === 'WARN' */
  warnings: EntityResult[];
}

export function aggregateByName(entityType: EntityType, entries: EntityResult[] | undefined): AggregatedEntityResult[] {
  if (!entries?.length) return [];

  const groups = new Map<string, EntityResult[]>();
  for (const e of entries) {
    const name = e.additionalData?.name ?? '';
    let list = groups.get(name);
    if (!list) {
      list = [];
      groups.set(name, list);
    }
    list.push(e);
  }

  return Array.from(groups, ([name, group]) => {
    const errors = group.filter(e => e.status === IssueStatus.ERROR);
    const warnings = group.filter(e => e.status === IssueStatus.WARN);
    const okEntry = group.find(e => e.status === IssueStatus.OK || (!!e.id && !e.error));
    const status: AggregatedEntityResult['status'] =
      errors.length > 0 ? IssueStatus.ERROR :
      warnings.length > 0 ? IssueStatus.WARN :
      IssueStatus.OK;
    return {
      key: `${entityType}:${name}`,
      primary: okEntry ?? group[0],
      status,
      errors,
      warnings,
    };
  });
}

/** Renders e.g. "1 error", "3 errors", "1 error, 2 warnings". */
export function issuesCountSummary(errors: EntityResult[], warnings: EntityResult[], translate: TranslateService): string {
  const parts: string[] = [];
  if (errors.length) {
    parts.push(errors.length === 1 ? `1 ${translate.instant('event.error')}` : `${errors.length} ${translate.instant('event.errors')}`);
  }
  if (warnings.length) {
    parts.push(warnings.length === 1 ? `1 ${translate.instant('event.warning')}` : `${warnings.length} ${translate.instant('event.warnings')}`);
  }
  return parts.join(', ');
}
