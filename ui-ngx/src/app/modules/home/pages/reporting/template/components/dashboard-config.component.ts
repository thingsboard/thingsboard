// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  DashboardReportComponentConfig,
  imageAlignments,
  imageAlignmentTranslations,
  imageWidthTypes,
  imageWidthTypeTranslations
} from '@shared/models/report-component.models';
import { WidgetConfigMode } from '@shared/models/widget.models';

@Component({
    selector: 'tb-dashboard-config',
    templateUrl: './dashboard-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DashboardConfigComponent extends AbstractReportComponentConfig<DashboardReportComponentConfig> {

  imageWidthTypes = imageWidthTypes;
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  settingsTab: 'dashboard' | 'layout' = 'dashboard';

  protected buildForm(reportComponentConfig: DashboardReportComponentConfig): FormGroup {
    const form = this.fb.group({
      dataSources: [reportComponentConfig.dataSources, []],
      config: [reportComponentConfig.config, []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []]
    });
    form.get('widthType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });
    return form;
  }

  private updateCustomWidth() {
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = 200;
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }
}
