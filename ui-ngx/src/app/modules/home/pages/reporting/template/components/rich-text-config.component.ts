// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { RichTextReportComponentConfig } from '@shared/models/report-component.models';

@Component({
    selector: 'tb-report-rich-text-config',
    templateUrl: './rich-text-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RichTextConfigComponent extends AbstractReportComponentConfig<RichTextReportComponentConfig> {

  settingsTab: 'content' | 'data' | 'layout' = 'content';

  protected buildForm(reportComponentConfig: RichTextReportComponentConfig): FormGroup {
    return this.fb.group({
      value: [reportComponentConfig.value, []],
      dataSources: [reportComponentConfig.dataSources, []]
    });
  }
}
