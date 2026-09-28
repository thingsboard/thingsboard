// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  pageOrientations,
  pageOrientationTranslationMap,
  pageSizes,
  paperSizeDisplayMap,
  ReportTemplateSettings,
  TbReportFormat
} from '@shared/models/report.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { deepTrim } from '@core/utils';

@Component({
    selector: 'tb-report-template-settings',
    templateUrl: './report-template-settings.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportTemplateSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportTemplateSettingsComponent implements OnInit, OnChanges, ControlValueAccessor {

  pageSizes = pageSizes;
  paperSizeDisplayMap = paperSizeDisplayMap;

  pageOrientations = pageOrientations;
  pageOrientationTranslationMap = pageOrientationTranslationMap;

  TbReportFormat = TbReportFormat;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  private modelValue: ReportTemplateSettings;

  private propagateChange = null;

  public settingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.settingsFormGroup = this.fb.group({
      name: [null, [Validators.required, Validators.maxLength(255)]],
      namePattern: [null, [Validators.required]],
      timeDataPattern: [null, []],
      description: [null, []],
      pageSize: [null, []],
      pageOrientation: [null, []],
      pageMargins: [null, []],
      pageBackground: [null, []]
    });
    this.settingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.updateValidators();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['subReport', 'format'].includes(propName)) {
          this.updateValidators();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.settingsFormGroup.disable({emitEvent: false});
    } else {
      this.settingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ReportTemplateSettings): void {
    this.modelValue = value;
    this.settingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateValidators() {
    if (this.subReport) {
      this.settingsFormGroup.get('namePattern').disable({emitEvent: false});
      this.settingsFormGroup.get('timeDataPattern').disable({emitEvent: false});
    } else {
      this.settingsFormGroup.get('namePattern').enable({emitEvent: false});
      this.settingsFormGroup.get('timeDataPattern').enable({emitEvent: false});
    }
    if (this.subReport || this.format !== TbReportFormat.PDF) {
      this.settingsFormGroup.get('pageSize').disable({emitEvent: false});
      this.settingsFormGroup.get('pageOrientation').disable({emitEvent: false});
      this.settingsFormGroup.get('pageMargins').disable({emitEvent: false});
      this.settingsFormGroup.get('pageBackground').disable({emitEvent: false});
    } else {
      this.settingsFormGroup.get('pageSize').enable({emitEvent: false});
      this.settingsFormGroup.get('pageOrientation').enable({emitEvent: false});
      this.settingsFormGroup.get('pageMargins').enable({emitEvent: false});
      this.settingsFormGroup.get('pageBackground').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.settingsFormGroup.value;
    this.propagateChange(deepTrim(this.modelValue));
  }
}
