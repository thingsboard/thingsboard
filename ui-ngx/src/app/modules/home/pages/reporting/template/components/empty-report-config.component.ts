// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import {
  reportComponentTypesData
} from '@home/pages/reporting/template/components/report-component.models';
import { ReportComponentConfig } from '@shared/models/report-component.models';

@Component({
    selector: 'tb-empty-report-config',
    templateUrl: './empty-report-config.component.html',
    styleUrls: ['./empty-report-config.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EmptyReportConfigComponent extends AbstractReportComponentConfig {

  public title: string;

  setupConfig(reportComponentConfig: ReportComponentConfig): FormGroup {
    this.title = reportComponentTypesData.getReportComponentTypeData(reportComponentConfig.type, reportComponentConfig.subType).title;
    return this.fb.group({});
  }

}
