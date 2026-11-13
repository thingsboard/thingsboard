// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { SplitViewReportComponentConfig } from '@shared/models/report-component.models';
import { FormGroup, Validators } from '@angular/forms';

@Component({
    selector: 'tb-split-view-config',
    templateUrl: './split-view-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SplitViewConfigComponent extends AbstractReportComponentConfig<SplitViewReportComponentConfig> {

  protected buildForm(reportComponentConfig: SplitViewReportComponentConfig): FormGroup {

    return this.fb.group({
      splitPosition: [reportComponentConfig.splitPosition, [Validators.min(1), Validators.max(99), Validators.required]],
      splitGap: [reportComponentConfig.splitGap, [Validators.min(0), Validators.required]],
      leftVerticalAlignment: [reportComponentConfig.leftVerticalAlignment, []],
      rightVerticalAlignment: [reportComponentConfig.rightVerticalAlignment, []]
    });
  }
}
