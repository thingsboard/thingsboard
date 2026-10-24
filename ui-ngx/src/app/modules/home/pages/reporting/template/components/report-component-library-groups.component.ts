// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbReportFormat } from '@shared/models/report.models';
import {
  ReportComponentContext,
  reportComponentLibraryGroups
} from '@home/pages/reporting/template/components/report-component.models';

@Component({
    selector: 'tb-report-component-library-groups',
    templateUrl: './report-component-library-groups.component.html',
    styleUrls: ['./report-component-library-groups.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentLibraryGroupsComponent {

  reportComponentLibraryGroups = reportComponentLibraryGroups;

  @Input()
  context: ReportComponentContext;

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  @Input()
  filter: string;

  constructor() {
  }
}
