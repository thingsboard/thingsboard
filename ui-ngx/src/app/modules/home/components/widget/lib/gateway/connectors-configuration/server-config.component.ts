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
  OnDestroy
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subject } from 'rxjs';
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
  BrokerSecurityType,
  noLeadTrailSpacesRegex,
  SecurityType,
  ServerSecurityTypes
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';
import { isDefined } from '@core/utils';

@Component({
  selector: 'tb-server-config',
  templateUrl: './server-config.component.html',
  styleUrls: ['./server-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ServerConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ServerConfigComponent),
      multi: true
    }
  ]
})
export class ServerConfigComponent extends PageComponent implements ControlValueAccessor, Validator, OnDestroy {

  serverSecurityTypes = ServerSecurityTypes;

  serverConfigFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.serverConfigFormGroup = this.fb.group({
      name: ['', []],
      url: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      timeoutInMillis: [0, []],
      scanPeriodInMillis: [0, []],
      enableSubscriptions: [true, []],
      subCheckPeriodInMillis: [0, []],
      showMap: [false, []],
      security: [SecurityType.BASIC128, []],
      identity: [{}, [Validators.required]]
    });

    this.serverConfigFormGroup.valueChanges.pipe(
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
    if (!deviceInfo) {
      deviceInfo = {};
    }
    if (!deviceInfo.security) {
      deviceInfo.security = SecurityType.BASIC128;
    }
    if (!deviceInfo.identity) {
      deviceInfo.identity = { type: BrokerSecurityType.ANONYMOUS };
    }
    if (!isDefined(deviceInfo.enableSubscriptions)) {
      deviceInfo.enableSubscriptions = true;
    }
    if (!isDefined(deviceInfo.showMap)) {
      deviceInfo.showMap = false;
    }
    this.serverConfigFormGroup.reset(deviceInfo);
    this.updateView(deviceInfo);
  }

  validate(): ValidationErrors | null {
    return this.serverConfigFormGroup.valid ? null : {
      serverConfigForm: { valid: false }
    };
  }

  updateView(value: any) {
    this.propagateChange(value);
  }
}
