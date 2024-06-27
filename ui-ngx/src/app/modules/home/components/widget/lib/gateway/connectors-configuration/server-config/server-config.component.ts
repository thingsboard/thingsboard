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
  forwardRef, inject, Input,
  OnDestroy, OnInit
} from '@angular/core';
import {
  ControlContainer,
  ControlValueAccessor,
  FormBuilder, FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  noLeadTrailSpacesRegex,
  SecurityType,
  ServerSecurityTypes
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration';

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
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
  ]
})
export class ServerConfigComponent implements OnInit, ControlValueAccessor, OnDestroy {
  @Input() controlKey = 'server';

  serverSecurityTypes = ServerSecurityTypes;
  serverConfigFormGroup: UntypedFormGroup;

  get parentFormGroup(): FormGroup {
    return this.parentContainer.control as FormGroup;
  }

  private parentContainer = inject(ControlContainer);

  constructor(private fb: FormBuilder) {
    this.serverConfigFormGroup = this.fb.group({
      name: ['', []],
      url: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      timeoutInMillis: [1000, [Validators.required, Validators.min(1000)]],
      scanPeriodInMillis: [1000, [Validators.required, Validators.min(1000)]],
      enableSubscriptions: [true, []],
      subCheckPeriodInMillis: [10, [Validators.required, Validators.min(10)]],
      showMap: [false, []],
      security: [SecurityType.BASIC128, []],
      identity: [{}, [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.addSelfControl();

  }

  ngOnDestroy(): void {
    this.removeSelfControl();
  }

  registerOnChange(fn: any): void {}

  registerOnTouched(fn: any): void {}

  writeValue(obj: any): void {}

  private addSelfControl(): void {
    this.parentFormGroup.addControl(this.controlKey,  this.serverConfigFormGroup);
  }

  private removeSelfControl(): void {
    this.parentFormGroup.removeControl(this.controlKey);
  }
}
