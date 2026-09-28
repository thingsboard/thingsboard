// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import {
  borderLengths,
  borderLengthTranslations,
  borderTypes,
  borderTypeTranslations,
  DividerReportComponentConfig
} from '@shared/models/report-component.models';

@Component({
    selector: 'tb-report-divider-config',
    templateUrl: './divider-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DividerConfigComponent extends AbstractReportComponentConfig<DividerReportComponentConfig> {

  settingsTab: 'content' | 'layout' = 'content';

  borderLengths = borderLengths;
  borderLengthTranslations = borderLengthTranslations;

  borderTypes = borderTypes;
  borderTypeTranslations = borderTypeTranslations;

  protected buildForm(reportComponentConfig: DividerReportComponentConfig): FormGroup {
    return this.fb.group({
      length: [reportComponentConfig.length, []],
      borderType: [reportComponentConfig.borderType, []],
      widthPx: [reportComponentConfig.widthPx, []],
      color: [reportComponentConfig.color, []]
    });
  }
}
