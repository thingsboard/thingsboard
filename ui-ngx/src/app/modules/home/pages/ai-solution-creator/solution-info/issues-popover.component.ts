// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { AggregatedEntityResult, issuesCountSummary } from '@home/pages/ai-solution-creator/solution-info/issues-aggregation';
import { EntityResult } from '@shared/models/solution-creator.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-issues-popover',
  templateUrl: './issues-popover.component.html',
  styleUrls: ['./issues-popover.component.scss'],
  standalone: false
})
export class IssuesPopoverComponent {

  @Input() entry!: AggregatedEntityResult;

  constructor(private translate: TranslateService) {}

  get hasErrors(): boolean { return this.entry.errors.length > 0; }
  get hasWarnings(): boolean { return this.entry.warnings.length > 0; }
  get showSections(): boolean { return this.hasErrors && this.hasWarnings; }

  get subtitle(): string {
    return issuesCountSummary(this.entry.errors, this.entry.warnings, this.translate);
  }

  getError(item: EntityResult): string {
    if (item.error?.message) {
      return item.error.message;
    }
    if (item.error?.errorType) {
      return this.translate.instant(
        item.error.errorType === 'TB' ? 'solution-creator.failed-install-entity' : 'solution-creator.failed-create-entity'
      );
    }
    return '';
  }
}
