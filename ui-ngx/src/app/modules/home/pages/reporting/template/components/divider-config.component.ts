// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import {
  BorderType,
  borderLengths,
  borderLengthTranslations,
  borderTypes,
  borderTypeTranslations,
  DividerReportComponentConfig
} from '@shared/models/report-component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const DOUBLE_BORDER_MIN_WIDTH_PX = 3;

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

  get widthPxMin(): number {
    return this.reportConfigForm?.get('borderType')?.value === BorderType.double ? DOUBLE_BORDER_MIN_WIDTH_PX : 0;
  }

  protected buildForm(reportComponentConfig: DividerReportComponentConfig): FormGroup {
    const form = this.fb.group({
      length: [reportComponentConfig.length, []],
      borderType: [reportComponentConfig.borderType, []],
      widthPx: [reportComponentConfig.widthPx, []],
      color: [reportComponentConfig.color, []]
    });

    form.get('borderType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((borderType: BorderType) => {
      this.applyWidthPxValidators(form, borderType);
    });

    this.applyWidthPxValidators(form, reportComponentConfig.borderType);

    return form;
  }

  private applyWidthPxValidators(form: FormGroup, borderType: BorderType): void {
    const widthPxControl = form.get('widthPx');
    if (borderType === BorderType.double) {
      widthPxControl.setValidators([Validators.min(DOUBLE_BORDER_MIN_WIDTH_PX)]);
      if (widthPxControl.value != null && widthPxControl.value < DOUBLE_BORDER_MIN_WIDTH_PX) {
        widthPxControl.setValue(DOUBLE_BORDER_MIN_WIDTH_PX);
      }
    } else {
      widthPxControl.setValidators([Validators.min(0)]);
    }
    widthPxControl.updateValueAndValidity();
  }
}
