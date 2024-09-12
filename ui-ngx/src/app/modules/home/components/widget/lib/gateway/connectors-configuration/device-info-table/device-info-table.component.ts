///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  DeviceInfoType,
  noLeadTrailSpacesRegex,
  OPCUaSourceType,
  SourceType,
  SourceTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-device-info-table',
  templateUrl: './device-info-table.component.html',
  styleUrls: ['./device-info-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceInfoTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceInfoTableComponent),
      multi: true
    }
  ]
})
export class DeviceInfoTableComponent extends PageComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  SourceTypeTranslationsMap = SourceTypeTranslationsMap;

  DeviceInfoType = DeviceInfoType;

  @coerceBoolean()
  @Input()
  useSource = true;

  @coerceBoolean()
  @Input()
  required = false;

  @Input()
  sourceTypes: Array<SourceType | OPCUaSourceType> = Object.values(SourceType);

  deviceInfoTypeValue: any;

  get deviceInfoType(): any {
    return this.deviceInfoTypeValue;
  }

  @Input()
  set deviceInfoType(value: any) {
    if (this.deviceInfoTypeValue !== value) {
      this.deviceInfoTypeValue = value;
    }
  }

  mappingFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public dialog: MatDialog,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.mappingFormGroup = this.fb.group({
      deviceNameExpression: ['', this.required ?
        [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)] : [Validators.pattern(noLeadTrailSpacesRegex)]]
    });

    if (this.useSource) {
      this.mappingFormGroup.addControl('deviceNameExpressionSource',
        this.fb.control(this.sourceTypes[0], []));
    }

    if (this.deviceInfoType === DeviceInfoType.FULL) {
      if (this.useSource) {
        this.mappingFormGroup.addControl('deviceProfileExpressionSource',
          this.fb.control(this.sourceTypes[0], []));
      }
      this.mappingFormGroup.addControl('deviceProfileExpression',
        this.fb.control('', this.required ?
          [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)] : [Validators.pattern(noLeadTrailSpacesRegex)]));
    }

    this.mappingFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any) {
    this.mappingFormGroup.patchValue(deviceInfo, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.mappingFormGroup.valid ? null : {
      mappingForm: { valid: false }
    };
  }

  updateView(value: any) {
    this.propagateChange(value);
  }
}
