// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { ReportTemplateType } from '@shared/models/report.models';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { SubReportReportComponentConfig } from '@shared/models/report-component.models';
import { WidgetConfigMode } from '@shared/models/widget.models';

@Component({
    selector: 'tb-sub-report-config',
    templateUrl: './sub-report-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SubReportConfigComponent extends AbstractReportComponentConfig<SubReportReportComponentConfig> {

  EntityType = EntityType;
  ReportTemplateType = ReportTemplateType;

  basicMode = WidgetConfigMode.basic;

  protected buildForm(reportComponentConfig: SubReportReportComponentConfig): FormGroup {
    const form: FormGroup = this.fb.group({
      dataSources: [reportComponentConfig.dataSources, []],
      templateId: [reportComponentConfig.templateId, []]
    });
    if (!this.isPlainFormat) {
      form.addControl('avoidPageBreakInside', this.fb.control(reportComponentConfig.avoidPageBreakInside, []));
    }
    return form;
  }
}
